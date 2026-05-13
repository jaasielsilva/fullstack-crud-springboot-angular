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
import com.clientes_api.util.MercadoPagoPreferenciaUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria assinatura {@link StatusAssinatura#PENDENTE} e preferência no Mercado Pago (valor só no backend).
 */
@Service
public class CheckoutMercadoPagoService {

    private final MercadoPagoApiService mercadoPagoApiService;
    private final EmpresaService empresaService;
    private final PlanoService planoService;
    private final AssinaturaService assinaturaService;
    private final ObjectMapper objectMapper;
    private final MercadoPagoValorPreferenciaService mercadoPagoValorPreferenciaService;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${mercadopago.prefer-sandbox-init-point:false}")
    private boolean preferSandboxInitPoint;

    public CheckoutMercadoPagoService(MercadoPagoApiService mercadoPagoApiService,
                                      EmpresaService empresaService,
                                      PlanoService planoService,
                                      AssinaturaService assinaturaService,
                                      ObjectMapper objectMapper,
                                      MercadoPagoValorPreferenciaService mercadoPagoValorPreferenciaService) {
        this.mercadoPagoApiService = mercadoPagoApiService;
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

        ObjectNode body = montarPreferencia(plano, empresa, externalReference, usuarioLogado);
        var response = mercadoPagoApiService.criarPreferencia(body);

        String prefId = response.path("id").asText(null);
        String checkoutUrl = extrairInitPoint(response);

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new BusinessException("Resposta do Mercado Pago sem URL de checkout (init_point).");
        }

        assinatura.setMercadoPagoPreferenceId(prefId);
        assinaturaService.salvar(assinatura);

        return new CheckoutResponseDTO(checkoutUrl, prefId);
    }

    private ObjectNode montarPreferencia(Plano plano, Tenant empresa, String externalReference, Usuario usuarioLogado) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", externalReference);
        item.put("category_id", MercadoPagoPreferenciaUtil.ITEM_CATEGORY_PADRAO);
        item.put("title", plano.getNome() + " - ERP Corporativo");
        item.put("description",
                plano.getDescricao() != null && !plano.getDescricao().isBlank()
                        ? plano.getDescricao()
                        : "Assinatura mensal para Adegas e Distribuidoras");
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", mercadoPagoValorPreferenciaService.resolverPrecoUnitario(plano.getValor()));

        ObjectNode payer = root.putObject("payer");
        String email = MercadoPagoPreferenciaUtil.primeiroEmailValido(empresa.getEmail(), usuarioLogado.getLogin());
        if (email == null) {
            throw new BusinessException(
                    "Cadastre um e-mail válido na empresa ou use login com e-mail para o checkout (exigência Mercado Pago).");
        }
        payer.put("email", email);

        root.put("external_reference", externalReference);
        root.put("notification_url", notificationUrl);

        ObjectNode backUrls = root.putObject("back_urls");
        backUrls.put("success", frontendUrl + "/pagamento/sucesso");
        backUrls.put("failure", frontendUrl + "/pagamento/falha");
        backUrls.put("pending", frontendUrl + "/pagamento/pendente");

        // auto_return só funciona com URLs públicas (não localhost)
        if (!frontendUrl.contains("localhost") && !frontendUrl.contains("127.0.0.1")) {
            root.put("auto_return", "approved");
        }
        return root;
    }

    private String extrairInitPoint(com.fasterxml.jackson.databind.JsonNode response) {
        String sandbox = response.path("sandbox_init_point").asText(null);
        String prod = response.path("init_point").asText(null);
        if (preferSandboxInitPoint && sandbox != null && !sandbox.isBlank()) {
            return sandbox;
        }
        if (prod != null && !prod.isBlank()) {
            return prod;
        }
        return sandbox;
    }
}
