package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Pagamento;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusPagamento;
import com.clientes_api.repository.UsuarioRepository;
import com.clientes_api.util.MercadoPagoExternalReference;
import com.clientes_api.util.PedidoMercadoPagoExternalReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Processa webhooks de pagamento do Mercado Pago com idempotência e validação de {@code external_reference}.
 * Libera acesso somente após status {@code approved} confirmado na API (não pelo redirect do frontend).
 */
@Service
public class MercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    private final MercadoPagoApiService mercadoPagoApiService;
    private final PagamentoService pagamentoService;
    private final AssinaturaService assinaturaService;
    private final EmpresaService empresaService;
    private final PedidoService pedidoService;
    private final AssinaturaAtivacaoService assinaturaAtivacaoService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;

    public MercadoPagoWebhookService(MercadoPagoApiService mercadoPagoApiService,
                                     PagamentoService pagamentoService,
                                     AssinaturaService assinaturaService,
                                     EmpresaService empresaService,
                                     PedidoService pedidoService,
                                     AssinaturaAtivacaoService assinaturaAtivacaoService,
                                     EmailService emailService,
                                     UsuarioRepository usuarioRepository) {
        this.mercadoPagoApiService = mercadoPagoApiService;
        this.pagamentoService = pagamentoService;
        this.assinaturaService = assinaturaService;
        this.empresaService = empresaService;
        this.pedidoService = pedidoService;
        this.assinaturaAtivacaoService = assinaturaAtivacaoService;
        this.emailService = emailService;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void processarPagamentoPorId(String mercadoPagoPaymentId) {
        JsonNode payment = mercadoPagoApiService.buscarPagamento(mercadoPagoPaymentId);
        String rawPayment = payment.toString();

        String mpStatus = payment.path("status").asText("unknown");

        if ("approved".equalsIgnoreCase(mpStatus)
                && pagamentoService.jaAprovado(mercadoPagoPaymentId)) {
            log.info("Webhook idempotente: pagamento {} já estava APPROVED.", mercadoPagoPaymentId);
            return;
        }

        String externalRef = payment.path("external_reference").asText(null);
        if (externalRef == null || externalRef.isBlank()) {
            log.warn("Pagamento MP {} sem external_reference; ignorando.", mercadoPagoPaymentId);
            return;
        }

        StatusPagamento statusPagamento = mapearStatusPagamento(mpStatus);

        var subOpt = MercadoPagoExternalReference.parse(externalRef);
        if (subOpt.isPresent()) {
            processarPagamentoAssinatura(subOpt.get(), mercadoPagoPaymentId, payment, rawPayment, statusPagamento, externalRef);
            return;
        }

        var pedOpt = PedidoMercadoPagoExternalReference.parse(externalRef);
        if (pedOpt.isPresent()) {
            processarPagamentoPedido(pedOpt.get(), mercadoPagoPaymentId, payment, rawPayment, statusPagamento, externalRef);
            return;
        }

        log.warn("external_reference não reconhecido (nem assinatura nem pedido): {} | payment={}", externalRef, mercadoPagoPaymentId);
    }

    private void processarPagamentoAssinatura(MercadoPagoExternalReference parsed,
                                              String mercadoPagoPaymentId,
                                              JsonNode payment,
                                              String rawPayment,
                                              StatusPagamento statusPagamento,
                                              String externalRef) {
        TenantContext.setCurrentTenant(parsed.empresaId());
        try {
            boolean enviarComprovante = "approved".equalsIgnoreCase(payment.path("status").asText(""))
                    && !pagamentoService.jaAprovado(mercadoPagoPaymentId);

            Assinatura assinatura = assinaturaService.buscarPorIdETenantOuErro(parsed.assinaturaId(), parsed.empresaId());
            if (assinatura.getPlano() == null || !assinatura.getPlano().getId().equals(parsed.planoId())) {
                throw new IllegalArgumentException("external_reference não confere com assinatura/plano persistidos");
            }

            Tenant empresa = empresaService.buscarPorIdOuErro(parsed.empresaId());

            Pagamento pagamento = pagamentoService.buscarPorMercadoPagoId(mercadoPagoPaymentId)
                    .orElseGet(Pagamento::new);
            pagamento.setTenantId(parsed.empresaId());
            pagamento.setAssinatura(assinatura);
            pagamento.setPedido(null);
            pagamento.setMercadoPagoPaymentId(mercadoPagoPaymentId);
            pagamento.setMercadoPagoPreferenceId(payment.path("preference_id").asText(null));
            pagamento.setStatus(statusPagamento);
            pagamento.setStatusDetail(payment.path("status_detail").asText(null));
            if (payment.hasNonNull("transaction_amount")) {
                pagamento.setValor(new BigDecimal(payment.path("transaction_amount").asText()));
            }
            pagamento.setMetodoPagamento(payment.path("payment_method_id").asText(null));
            pagamento.setExternalReference(externalRef);
            pagamento.setPayloadJson(rawPayment);
            pagamentoService.salvar(pagamento);

            LocalDateTime agora = LocalDateTime.now();
            assinaturaAtivacaoService.aplicarResultadoPagamentoMercadoPago(
                    payment.path("status").asText("unknown"), assinatura, empresa, agora, mercadoPagoPaymentId);

            assinaturaService.salvar(assinatura);
            empresaService.salvar(empresa);

            if (enviarComprovante) {
                notificarComprovanteMercadoPago(empresa, assinatura, pagamento, mercadoPagoPaymentId);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void notificarComprovanteMercadoPago(
            Tenant empresa,
            Assinatura assinatura,
            Pagamento pagamento,
            String mercadoPagoPaymentId
    ) {
        String destino = resolverEmailComprovante(empresa);
        if (destino == null) {
            log.warn("Mercado Pago webhook | comprovante não enviado: nenhum e-mail da empresa ou do primeiro usuário");
            return;
        }
        String planoNome = assinatura.getPlano() != null ? assinatura.getPlano().getNome() : null;
        emailService.enviarComprovantePagamentoAssinatura(
                destino,
                empresa.getNome(),
                planoNome,
                pagamento.getValor(),
                "Mercado Pago",
                mercadoPagoPaymentId
        );
    }

    private String resolverEmailComprovante(Tenant empresa) {
        if (empresa.getEmail() != null && !empresa.getEmail().isBlank()) {
            return empresa.getEmail().trim();
        }
        return usuarioRepository.findFirstByTenantIdOrderByIdAsc(empresa.getId())
                .map(Usuario::getLogin)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .orElse(null);
    }

    private void processarPagamentoPedido(PedidoMercadoPagoExternalReference ref,
                                         String mercadoPagoPaymentId,
                                         JsonNode payment,
                                         String rawPayment,
                                         StatusPagamento statusPagamento,
                                         String externalRef) {
        TenantContext.setCurrentTenant(ref.tenantId());
        try {
            pedidoService.processarWebhookMercadoPagoPedido(
                    ref.pedidoId(), mercadoPagoPaymentId, payment, rawPayment, externalRef, statusPagamento);
        } finally {
            TenantContext.clear();
        }
    }

    private StatusPagamento mapearStatusPagamento(String mpStatus) {
        if (mpStatus == null) {
            return StatusPagamento.UNKNOWN;
        }
        return switch (mpStatus.toLowerCase()) {
            case "approved" -> StatusPagamento.APPROVED;
            case "pending" -> StatusPagamento.PENDING;
            case "in_process" -> StatusPagamento.IN_PROCESS;
            case "rejected" -> StatusPagamento.REJECTED;
            case "cancelled" -> StatusPagamento.CANCELLED;
            case "refunded" -> StatusPagamento.REFUNDED;
            case "charged_back" -> StatusPagamento.CHARGED_BACK;
            default -> StatusPagamento.UNKNOWN;
        };
    }
}
