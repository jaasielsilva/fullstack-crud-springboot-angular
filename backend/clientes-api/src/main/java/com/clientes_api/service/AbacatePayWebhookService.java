package com.clientes_api.service;

import com.clientes_api.config.TenantContext;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Pagamento;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusPagamento;
import com.clientes_api.util.AbacatePagamentoIds;
import com.clientes_api.util.MercadoPagoExternalReference;
import com.clientes_api.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Webhook Abacate Pay — confirma checkout pago, grava {@link com.clientes_api.model.Pagamento} (status aprovado)
 * e ativa assinatura (mesmo {@code externalId} do fluxo MP).
 */
@Service
public class AbacatePayWebhookService {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayWebhookService.class);

    private static final int MAX_WEBHOOK_LOG_PREVIEW = 1500;

    private final ObjectMapper objectMapper;
    private final AssinaturaService assinaturaService;
    private final EmpresaService empresaService;
    private final AssinaturaAtivacaoService assinaturaAtivacaoService;
    private final PagamentoService pagamentoService;
    private final EmailService emailService;
    private final UsuarioRepository usuarioRepository;

    @Value("${abacatepay.webhook-secret:}")
    private String configuredWebhookSecret;

    @Value("${abacatepay.webhook-hmac-key:}")
    private String webhookHmacKey;

    @Value("${abacatepay.log-webhook-inbound:false}")
    private boolean logWebhookInbound;

    public AbacatePayWebhookService(ObjectMapper objectMapper,
                                    AssinaturaService assinaturaService,
                                    EmpresaService empresaService,
                                    AssinaturaAtivacaoService assinaturaAtivacaoService,
                                    PagamentoService pagamentoService,
                                    EmailService emailService,
                                    UsuarioRepository usuarioRepository) {
        this.objectMapper = objectMapper;
        this.assinaturaService = assinaturaService;
        this.empresaService = empresaService;
        this.assinaturaAtivacaoService = assinaturaAtivacaoService;
        this.pagamentoService = pagamentoService;
        this.emailService = emailService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Valida segredo/assinatura (se configurados), processa {@code checkout.completed} com status {@code PAID}.
     *
     * @return {@code false} se segredo/HMAC inválidos ou erro inesperado (não marcar idempotência);
     *         {@code true} se o evento foi tratado até o fim (incluindo ignorados de propósito).
     */
    @Transactional
    public boolean processar(String rawBody, String webhookSecretQuery, String signatureHeader) {
        if (logWebhookInbound) {
            int len = rawBody == null ? 0 : rawBody.length();
            log.info("Abacate Pay webhook | POST | bodyLen={} | preview={}", len, previewBody(rawBody));
        }

        if (!validarWebhookSecret(webhookSecretQuery)) {
            log.warn("Abacate Pay webhook | webhookSecret inválido ou ausente (configurado no servidor)");
            return false;
        }

        if (!validarAssinaturaHmac(rawBody, signatureHeader)) {
            log.warn("Abacate Pay webhook | assinatura HMAC inválida ou ausente");
            return false;
        }

        if (rawBody == null || rawBody.isBlank()) {
            return true;
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String eventName = root.path("event").asText("").trim();
            if (!"checkout.completed".equalsIgnoreCase(eventName)) {
                if (logWebhookInbound) {
                    log.info("Abacate Pay webhook | ignorado | event={}", eventName);
                }
                return true;
            }

            JsonNode checkout = root.path("data").path("checkout");
            if (checkout.isMissingNode() || checkout.isNull()) {
                checkout = root.path("data");
            }

            String externalId = checkout.path("externalId").asText(null);
            if (externalId == null || externalId.isBlank()) {
                externalId = checkout.path("external_id").asText(null);
            }

            String status = checkout.path("status").asText("");
            String checkoutId = checkout.path("id").asText(null);

            if (!"PAID".equalsIgnoreCase(status)) {
                if (logWebhookInbound) {
                    log.info("Abacate Pay webhook | ignorado | status={} | externalId={}", status, externalId);
                }
                return true;
            }

            var refOpt = MercadoPagoExternalReference.parse(externalId);
            if (refOpt.isEmpty()) {
                log.warn("Abacate Pay webhook | externalId não reconhecido: {}", externalId);
                return true;
            }

            MercadoPagoExternalReference parsed = refOpt.get();
            TenantContext.setCurrentTenant(parsed.empresaId());
            try {
                Assinatura assinatura = assinaturaService.buscarPorIdETenantOuErro(parsed.assinaturaId(), parsed.empresaId());
                if (assinatura.getPlano() == null || !assinatura.getPlano().getId().equals(parsed.planoId())) {
                    throw new IllegalArgumentException("externalId não confere com assinatura/plano persistidos");
                }

                if (checkoutId != null && !checkoutId.isBlank()
                        && assinatura.getAbacatePayBillingId() != null && !assinatura.getAbacatePayBillingId().isBlank()
                        && !checkoutId.equals(assinatura.getAbacatePayBillingId())) {
                    log.warn(
                            "Abacate Pay webhook | id do checkout difere do billing salvo | payload={} | salvo={}",
                            checkoutId,
                            assinatura.getAbacatePayBillingId()
                    );
                    return true;
                }

                Tenant empresa = empresaService.buscarPorIdOuErro(parsed.empresaId());
                String chavePagamento = AbacatePagamentoIds.chavePagamento(checkoutId, externalId);
                boolean primeiroComprovante = pagamentoService.buscarPorMercadoPagoId(chavePagamento)
                        .map(p -> p.getStatus() != StatusPagamento.APPROVED)
                        .orElse(true);

                Pagamento pagamentoGravado = registrarPagamentoAbacate(
                        parsed, assinatura, rawBody, chavePagamento, externalId, checkout);

                LocalDateTime agora = LocalDateTime.now();
                assinaturaAtivacaoService.ativarAssinaturaEEmpresa(assinatura, empresa, agora, null);
                assinaturaService.salvar(assinatura);
                empresaService.salvar(empresa);

                if (primeiroComprovante) {
                    notificarComprovantePorEmail(empresa, assinatura, pagamentoGravado, checkoutId, externalId);
                }

                if (logWebhookInbound) {
                    log.info("Abacate Pay webhook | assinatura ativada | empresaId={} | assinaturaId={}", parsed.empresaId(), parsed.assinaturaId());
                }
                return true;
            } finally {
                TenantContext.clear();
            }
        } catch (Exception e) {
            log.error("Abacate Pay webhook | falha ao processar: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Persiste pagamento aprovado (mesma tabela do Mercado Pago; chave sintética em {@code mercado_pago_payment_id}).
     */
    private Pagamento registrarPagamentoAbacate(
            MercadoPagoExternalReference parsed,
            Assinatura assinatura,
            String rawBody,
            String chavePagamento,
            String externalId,
            JsonNode checkout
    ) {
        Pagamento p = pagamentoService.buscarPorMercadoPagoId(chavePagamento).orElseGet(Pagamento::new);
        p.setTenantId(parsed.empresaId());
        p.setAssinatura(assinatura);
        p.setPedido(null);
        p.setMercadoPagoPaymentId(chavePagamento);
        p.setMercadoPagoPreferenceId(null);
        p.setStatus(StatusPagamento.APPROVED);
        p.setStatusDetail("abacatepay:checkout.completed:PAID");
        BigDecimal valor = extrairValorCheckout(checkout);
        if (valor == null && assinatura.getPlano() != null) {
            valor = assinatura.getPlano().getValor();
        }
        p.setValor(valor);
        p.setMetodoPagamento("ABACATE_PAY");
        p.setExternalReference(externalId);
        p.setPayloadJson(rawBody);
        pagamentoService.salvar(p);
        return p;
    }

    private void notificarComprovantePorEmail(
            Tenant empresa,
            Assinatura assinatura,
            Pagamento pagamento,
            String checkoutId,
            String externalId
    ) {
        String destino = resolverEmailComprovante(empresa);
        if (destino == null) {
            log.warn("Abacate Pay webhook | comprovante não enviado: nenhum e-mail da empresa ou do primeiro usuário");
            return;
        }
        String planoNome = assinatura.getPlano() != null ? assinatura.getPlano().getNome() : null;
        String ref = (checkoutId != null && !checkoutId.isBlank()) ? checkoutId : externalId;
        emailService.enviarComprovantePagamentoAssinatura(
                destino,
                empresa.getId(),
                empresa.getNome(),
                planoNome,
                pagamento.getValor(),
                "Abacate Pay",
                ref
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

    private static BigDecimal extrairValorCheckout(JsonNode checkout) {
        if (checkout == null || checkout.isMissingNode()) {
            return null;
        }
        BigDecimal v = extrairBigDecimalCampo(checkout, "amount");
        if (v != null) {
            return v;
        }
        v = extrairBigDecimalCampo(checkout, "total");
        if (v != null) {
            return v;
        }
        return extrairBigDecimalCampo(checkout, "paidAmount");
    }

    private static BigDecimal extrairBigDecimalCampo(JsonNode checkout, String field) {
        if (!checkout.hasNonNull(field)) {
            return null;
        }
        JsonNode n = checkout.get(field);
        if (n.isNumber()) {
            return n.decimalValue();
        }
        String t = n.asText(null);
        if (t == null || t.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean validarWebhookSecret(String querySecret) {
        String expected = configuredWebhookSecret == null ? "" : configuredWebhookSecret.trim();
        if (expected.isEmpty()) {
            return true;
        }
        String received = querySecret == null ? "" : querySecret.trim();
        return constantTimeEquals(expected, received);
    }

    private boolean validarAssinaturaHmac(String rawBody, String signatureHeader) {
        String key = webhookHmacKey == null ? "" : webhookHmacKey.trim();
        if (key.isEmpty()) {
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank() || rawBody == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(sig);
            return constantTimeEquals(expected.trim(), signatureHeader.trim());
        } catch (Exception e) {
            log.warn("Abacate Pay webhook | erro ao calcular HMAC: {}", e.getMessage());
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            return false;
        }
        return MessageDigest.isEqual(ba, bb);
    }

    private static String previewBody(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            return "";
        }
        String t = rawBody.trim();
        if (t.length() <= MAX_WEBHOOK_LOG_PREVIEW) {
            return t;
        }
        return t.substring(0, MAX_WEBHOOK_LOG_PREVIEW) + "...";
    }
}
