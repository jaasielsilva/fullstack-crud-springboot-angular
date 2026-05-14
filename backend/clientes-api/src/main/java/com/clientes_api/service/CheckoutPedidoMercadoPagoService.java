package com.clientes_api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clientes_api.dto.CheckoutResponseDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Pedido;
import com.clientes_api.model.StatusPedido;
import com.clientes_api.repository.PedidoRepository;
import com.clientes_api.util.PedidoMercadoPagoExternalReference;
import com.clientes_api.util.MercadoPagoPreferenciaUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;

/**
 * Gera preferência Mercado Pago para pagamento total de um pedido B2B (external_reference {@link PedidoMercadoPagoExternalReference}).
 */
@Service
public class CheckoutPedidoMercadoPagoService {

    private final MercadoPagoApiService mercadoPagoApiService;
    private final PedidoRepository pedidoRepository;
    private final ObjectMapper objectMapper;
    private final MercadoPagoValorPreferenciaService mercadoPagoValorPreferenciaService;
    private final EmpresaService empresaService;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${mercadopago.prefer-sandbox-init-point:false}")
    private boolean preferSandboxInitPoint;

    @Value("${mercadopago.omit-callback-fields-without-https:false}")
    private boolean omitCallbackFieldsWithoutHttps;

    public CheckoutPedidoMercadoPagoService(MercadoPagoApiService mercadoPagoApiService,
                                            PedidoRepository pedidoRepository,
                                            ObjectMapper objectMapper,
                                            MercadoPagoValorPreferenciaService mercadoPagoValorPreferenciaService,
                                            EmpresaService empresaService) {
        this.mercadoPagoApiService = mercadoPagoApiService;
        this.pedidoRepository = pedidoRepository;
        this.objectMapper = objectMapper;
        this.mercadoPagoValorPreferenciaService = mercadoPagoValorPreferenciaService;
        this.empresaService = empresaService;
    }

    @Transactional
    public CheckoutResponseDTO criarCheckoutPedido(Long pedidoId, long tenantId) {
        Pedido pedido = pedidoRepository.findByIdAndTenantId(pedidoId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new BusinessException("Só é possível gerar cobrança para pedidos em aberto.");
        }
        if (pedido.getValorTotal() == null || pedido.getValorTotal() <= 0) {
            throw new BusinessException("Pedido sem valor total para cobrança.");
        }

        String externalReference = PedidoMercadoPagoExternalReference.format(tenantId, pedidoId);
        ObjectNode body = montarPreferencia(pedido, externalReference, tenantId);
        var response = mercadoPagoApiService.criarPreferencia(body);

        String prefId = response.path("id").asText(null);
        String checkoutUrl = MercadoPagoPreferenciaUtil.extrairInitPointPreferencia(response, preferSandboxInitPoint);

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new BusinessException("Resposta do Mercado Pago sem URL de checkout (init_point).");
        }

        pedido.setMercadoPagoPreferenceId(prefId);
        pedidoRepository.save(pedido);

        return new CheckoutResponseDTO(checkoutUrl, prefId);
    }

    private ObjectNode montarPreferencia(Pedido pedido, String externalReference, long tenantId) {
        ObjectNode root = objectMapper.createObjectNode();

        var empresa = empresaService.buscarPorIdOuErro(tenantId);

        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", externalReference);
        item.put("category_id", MercadoPagoPreferenciaUtil.ITEM_CATEGORY_PADRAO);
        item.put("title", "Pedido #" + pedido.getId() + " — ERP");
        String nomeCliente = pedido.getCliente().getNome() != null ? pedido.getCliente().getNome().trim() : "Cliente";
        item.put(
                "description",
                "Pagamento total do pedido #" + pedido.getId() + " — " + nomeCliente
                        + " — referência " + externalReference
        );

        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        BigDecimal valorPedido = BigDecimal.valueOf(pedido.getValorTotal());
        item.put("unit_price", mercadoPagoValorPreferenciaService.resolverPrecoUnitario(valorPedido));

        ObjectNode payer = root.putObject("payer");
        String email = MercadoPagoPreferenciaUtil.primeiroEmailValido(
                pedido.getCliente().getEmail(),
                empresa.getEmail());
        if (email == null) {
            throw new BusinessException(
                    "Cadastre um e-mail válido no cliente ou na empresa para gerar cobrança (exigência Mercado Pago).");
        }
        payer.put("email", email);
        MercadoPagoPreferenciaUtil.preencherPayerNome(payer, nomeCliente);
        MercadoPagoPreferenciaUtil.preencherPayerTelefoneBrasil(payer, pedido.getCliente().getTelefone());
        MercadoPagoPreferenciaUtil.preencherPayerIdentificacaoBrasil(payer, empresa.getDocumento());

        root.put("external_reference", externalReference);

        boolean incluirCallbacksHttps = !omitCallbackFieldsWithoutHttps
                || (MercadoPagoPreferenciaUtil.urlAbsolutaHttps(notificationUrl)
                && MercadoPagoPreferenciaUtil.urlAbsolutaHttps(frontendUrl));
        if (incluirCallbacksHttps) {
            root.put("notification_url", notificationUrl == null ? "" : notificationUrl.trim());

            ObjectNode backUrls = root.putObject("back_urls");
            backUrls.put("success", frontendUrl + "/pedidos?mp=success");
            backUrls.put("failure", frontendUrl + "/pedidos?mp=failure");
            backUrls.put("pending", frontendUrl + "/pedidos?mp=pending");

            if (!frontendUrl.contains("localhost") && !frontendUrl.contains("127.0.0.1")) {
                root.put("auto_return", "approved");
            }
        }

        ObjectNode metadata = root.putObject("metadata");
        metadata.put("tipo", "pedido");
        metadata.put("tenant_id", String.valueOf(tenantId));
        metadata.put("pedido_id", String.valueOf(pedido.getId()));

        MercadoPagoPreferenciaUtil.assertPreferenciaConformidade(root, incluirCallbacksHttps);
        return root;
    }
}
