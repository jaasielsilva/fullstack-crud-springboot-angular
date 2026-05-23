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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria assinatura {@link StatusAssinatura#PENDENTE} e checkout na AbacatePay.
 * <p>
 * O produto na AbacatePay deve ser pré-cadastrado no dashboard e seu ID
 * configurado via {@code abacatepay.product-id.<tipo>} (ex.:
 * {@code abacatepay.product-id.basico}).
 * O valor do plano é definido no produto cadastrado na AbacatePay — nunca
 * exposto ao frontend.
 */
@Service
public class CheckoutAbacatePayService {

    private final AbacatePayApiService abacatePayApiService;
    private final EmpresaService empresaService;
    private final PlanoService planoService;
    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${abacatepay.product-id.basico:}")
    private String productIdBasico;

    @Value("${abacatepay.product-id.premium:}")
    private String productIdPremium;

    public CheckoutAbacatePayService(AbacatePayApiService abacatePayApiService,
            EmpresaService empresaService,
            PlanoService planoService,
            AssinaturaService assinaturaService,
            ObjectMapper objectMapper) {
        this.abacatePayApiService = abacatePayApiService;
        this.empresaService = empresaService;
        this.planoService = planoService;
        this.assinaturaService = assinaturaService;
        this.objectMapper = objectMapper;
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

        String abacateProductId = resolverProductId(plano.getTipo());

        // Cria assinatura PENDENTE antes de chamar a API de pagamento
        Assinatura assinatura = new Assinatura();
        assinatura.setTenantId(req.empresaId());
        assinatura.setPlano(plano);
        assinatura.setStatus(StatusAssinatura.PENDENTE);
        assinatura = assinaturaService.salvar(assinatura);

        // externalId no formato compatível com o parser, adicionando sufixo para evitar conflito após reset do DB local
        String externalId = "EMPRESA_" + req.empresaId()
                + "_PLANO_" + plano.getId()
                + "_ASSINATURA_" + assinatura.getId()
                + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        assinatura.setExternalReference(externalId);
        assinatura = assinaturaService.salvar(assinatura);

        ObjectNode body = montarCorpoCheckout(abacateProductId, externalId, empresa);
        var data = abacatePayApiService.criarCheckout(body);

        String checkoutId = data.path("id").asText(null);
        String checkoutUrl = data.path("url").asText(null);

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new BusinessException("AbacatePay não retornou URL de checkout.");
        }

        // Persiste o ID do checkout na assinatura para rastreamento
        assinatura.setMercadoPagoPreferenceId(checkoutId); // campo reutilizado para o ID do checkout
        assinaturaService.salvar(assinatura);

        return new CheckoutResponseDTO(checkoutUrl, checkoutId);
    }

    private String resolverProductId(TipoPlano tipo) {
        String id = switch (tipo) {
            case BASICO -> productIdBasico;
            case PREMIUM -> productIdPremium;
            default -> null;
        };
        if (id == null || id.isBlank()) {
            throw new BusinessException(
                    "Produto AbacatePay não configurado para o plano " + tipo.name()
                            + ". Configure abacatepay.product-id." + tipo.name().toLowerCase());
        }
        return id;
    }

    private ObjectNode montarCorpoCheckout(String productId, String externalId, Tenant empresa) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", productId);
        item.put("quantity", 1);

        root.put("externalId", externalId);
        root.put("returnUrl", frontendUrl + "/pagamento/falha");
        root.put("completionUrl", frontendUrl + "/pagamento/sucesso");

        // Produtos de assinatura na AbacatePay exigem cartão (API 400 se incluir PIX).
        ArrayNode methods = root.putArray("methods");
        methods.add("CARD");

        ObjectNode metadata = root.putObject("metadata");
        metadata.put("tenant_id", empresa.getId());
        metadata.put("empresa_nome", empresa.getNome());

        return root;
    }
}
