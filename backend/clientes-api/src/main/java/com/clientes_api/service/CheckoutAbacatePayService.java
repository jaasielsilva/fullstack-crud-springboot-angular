package com.clientes_api.service;

import com.clientes_api.dto.CheckoutRequestDTO;
import com.clientes_api.dto.CheckoutResponseDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.model.Assinatura;
import com.clientes_api.model.Plano;
import com.clientes_api.model.Tenant;
import com.clientes_api.model.Usuario;
import com.clientes_api.model.enums.StatusAssinatura;
import com.clientes_api.model.enums.StatusEmpresa;
import com.clientes_api.model.enums.TipoPlano;
import com.clientes_api.util.MercadoPagoExternalReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Checkout de assinatura via Abacate Pay API <strong>v2</strong> (produto avulso + checkout hospedado).
 * Confirmação de pagamento: {@code POST /api/webhooks/abacatepay} ({@code checkout.completed} / {@code PAID}).
 */
@Service
public class CheckoutAbacatePayService {

    private final AbacatePayApiService abacatePayApiService;
    private final EmpresaService empresaService;
    private final PlanoService planoService;
    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;
    private final MercadoPagoValorPreferenciaService mercadoPagoValorPreferenciaService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public CheckoutAbacatePayService(AbacatePayApiService abacatePayApiService,
                                     EmpresaService empresaService,
                                     PlanoService planoService,
                                     AssinaturaService assinaturaService,
                                     ObjectMapper objectMapper,
                                     MercadoPagoValorPreferenciaService mercadoPagoValorPreferenciaService) {
        this.abacatePayApiService = abacatePayApiService;
        this.empresaService = empresaService;
        this.planoService = planoService;
        this.assinaturaService = assinaturaService;
        this.objectMapper = objectMapper;
        this.mercadoPagoValorPreferenciaService = mercadoPagoValorPreferenciaService;
    }

    @Transactional
    public CheckoutResponseDTO criarCheckout(CheckoutRequestDTO req, Usuario usuarioLogado) {
        if (!usuarioLogado.getTenantId().equals(req.empresaId())) {
            throw new BusinessException("Você não pode iniciar checkout para outra empresa.");
        }
        if (empresaService.isMatriz(req.empresaId())) {
            throw new BusinessException("Tenant matriz não requer checkout comercial.");
        }

        Tenant empresa = empresaService.buscarPorIdOuErro(req.empresaId());
        if (empresa.getStatus() == StatusEmpresa.CANCELADA) {
            throw new BusinessException("Empresa cancelada não pode contratar planos.");
        }

        Plano plano = planoService.buscarPorIdOuErro(req.planoId());
        if (!Boolean.TRUE.equals(plano.getAtivo())) {
            throw new BusinessException("Plano indisponível.");
        }
        if (!(plano.getTipo() == TipoPlano.BASICO || plano.getTipo() == TipoPlano.PREMIUM)) {
            throw new BusinessException("Tipo de plano não suportado para checkout.");
        }

        Assinatura assinatura = new Assinatura();
        assinatura.setTenantId(req.empresaId());
        assinatura.setPlano(plano);
        assinatura.setStatus(StatusAssinatura.PENDENTE);
        assinatura = assinaturaService.salvar(assinatura);

        String externalReference = MercadoPagoExternalReference.format(
                req.empresaId(),
                plano.getId(),
                assinatura.getId()
        );
        assinatura.setExternalReference(externalReference);
        assinatura = assinaturaService.salvar(assinatura);

        long precoCentavos = resolverPrecoCentavos(plano);

        ObjectNode produtoBody = montarProdutoPlano(plano, externalReference, precoCentavos);
        JsonNode produtoRoot = abacatePayApiService.criarProduto(produtoBody);
        String productId = produtoRoot.path("data").path("id").asText(null);
        if (productId == null || productId.isBlank()) {
            throw new BusinessException("Abacate Pay não retornou o id público do produto (data.id).");
        }

        ObjectNode checkoutBody = montarCheckout(productId, externalReference, empresa, plano, assinatura.getId());
        JsonNode checkoutRoot = abacatePayApiService.criarCheckout(checkoutBody);
        JsonNode data = checkoutRoot.path("data");
        String url = data.path("url").asText(null);
        String billingId = data.path("id").asText(null);
        if (url == null || url.isBlank()) {
            throw new BusinessException("Abacate Pay não retornou URL de pagamento (data.url).");
        }

        assinatura.setAbacatePayBillingId(billingId);
        assinaturaService.salvar(assinatura);

        return new CheckoutResponseDTO(url, billingId != null ? billingId : "");
    }

    private long resolverPrecoCentavos(Plano plano) {
        BigDecimal precoReais = mercadoPagoValorPreferenciaService.resolverPrecoUnitario(plano.getValor());
        long precoCentavos = precoReais.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        if (precoCentavos < 100) {
            precoCentavos = 100;
        }
        return precoCentavos;
    }

    private ObjectNode montarProdutoPlano(Plano plano, String externalReference, long precoCentavos) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("externalId", externalReference);
        root.put("name", plano.getNome() + " — ERP Corporativo");
        root.put(
                "description",
                plano.getDescricao() != null && !plano.getDescricao().isBlank()
                        ? plano.getDescricao()
                        : "Assinatura mensal"
        );
        root.put("price", precoCentavos);
        root.put("currency", "BRL");
        return root;
    }

    private ObjectNode montarCheckout(
            String productIdAbacate,
            String externalReference,
            Tenant empresa,
            Plano plano,
            long assinaturaId
    ) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", productIdAbacate);
        item.put("quantity", 1);

        ArrayNode methods = root.putArray("methods");
        methods.add("PIX");
        methods.add("CARD");

        root.put("returnUrl", frontendUrl + "/planos");
        root.put("completionUrl", frontendUrl + "/pagamento/sucesso?gateway=abacate");
        root.put("externalId", externalReference);

        ObjectNode metadata = root.putObject("metadata");
        metadata.put("empresa_id", String.valueOf(empresa.getId()));
        metadata.put("plano_id", String.valueOf(plano.getId()));
        metadata.put("assinatura_id", String.valueOf(assinaturaId));
        metadata.put("gateway", "abacatepay");

        return root;
    }
}
