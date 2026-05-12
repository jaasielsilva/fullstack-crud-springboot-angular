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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Gera preferência Mercado Pago para pagamento total de um pedido B2B (external_reference {@link PedidoMercadoPagoExternalReference}).
 */
@Service
public class CheckoutPedidoMercadoPagoService {

    private final MercadoPagoApiService mercadoPagoApiService;
    private final PedidoRepository pedidoRepository;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.notification-url}")
    private String notificationUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${mercadopago.prefer-sandbox-init-point:false}")
    private boolean preferSandboxInitPoint;

    public CheckoutPedidoMercadoPagoService(MercadoPagoApiService mercadoPagoApiService,
                                            PedidoRepository pedidoRepository,
                                            ObjectMapper objectMapper) {
        this.mercadoPagoApiService = mercadoPagoApiService;
        this.pedidoRepository = pedidoRepository;
        this.objectMapper = objectMapper;
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
        ObjectNode body = montarPreferencia(pedido, externalReference);
        var response = mercadoPagoApiService.criarPreferencia(body);

        String prefId = response.path("id").asText(null);
        String checkoutUrl = extrairInitPoint(response);

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new BusinessException("Resposta do Mercado Pago sem URL de checkout (init_point).");
        }

        pedido.setMercadoPagoPreferenceId(prefId);
        pedidoRepository.save(pedido);

        return new CheckoutResponseDTO(checkoutUrl, prefId);
    }

    private ObjectNode montarPreferencia(Pedido pedido, String externalReference) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("title", "Pedido #" + pedido.getId() + " — ERP");
        item.put("description", "Pagamento do pedido para " + pedido.getCliente().getNome());
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", pedido.getValorTotal());

        ObjectNode payer = root.putObject("payer");
        payer.put("name", pedido.getCliente().getNome());
        String email = pedido.getCliente().getEmail();
        payer.put("email", email != null && !email.isBlank() ? email : "nao-informado@placeholder.local");

        root.put("external_reference", externalReference);
        root.put("notification_url", notificationUrl);

        ObjectNode backUrls = root.putObject("back_urls");
        backUrls.put("success", frontendUrl + "/pedidos?mp=success");
        backUrls.put("failure", frontendUrl + "/pedidos?mp=failure");
        backUrls.put("pending", frontendUrl + "/pedidos?mp=pending");

        root.put("auto_return", "approved");
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
