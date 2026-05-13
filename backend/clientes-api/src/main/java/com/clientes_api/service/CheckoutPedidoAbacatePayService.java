package com.clientes_api.service;

import com.clientes_api.dto.CheckoutResponseDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Pedido;
import com.clientes_api.model.StatusPedido;
import com.clientes_api.repository.PedidoRepository;
import com.clientes_api.util.PedidoMercadoPagoExternalReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera checkout AbacatePay para pagamento de um pedido B2B.
 * <p>
 * Como pedidos têm valores variáveis, cria um produto avulso (ONE_TIME) na
 * AbacatePay
 * com o valor exato do pedido e em seguida cria o checkout com esse produto.
 */
@Service
public class CheckoutPedidoAbacatePayService {

    private final AbacatePayApiService abacatePayApiService;
    private final PedidoRepository pedidoRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public CheckoutPedidoAbacatePayService(AbacatePayApiService abacatePayApiService,
            PedidoRepository pedidoRepository,
            ObjectMapper objectMapper) {
        this.abacatePayApiService = abacatePayApiService;
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

        // Cria produto avulso na AbacatePay com o valor exato do pedido (em centavos)
        long valorCentavos = Math.round(pedido.getValorTotal() * 100);
        String externalId = PedidoMercadoPagoExternalReference.format(tenantId, pedidoId);
        String produtoAbacateId = criarProdutoTemporario(pedidoId, valorCentavos, externalId);

        // Cria o checkout com o produto recém-criado
        ObjectNode body = montarCorpoCheckout(produtoAbacateId, externalId, pedidoId);
        JsonNode data = abacatePayApiService.criarCheckout(body);

        String checkoutId = data.path("id").asText(null);
        String checkoutUrl = data.path("url").asText(null);

        if (checkoutUrl == null || checkoutUrl.isBlank()) {
            throw new BusinessException("AbacatePay não retornou URL de checkout.");
        }

        pedido.setMercadoPagoPreferenceId(checkoutId);
        pedidoRepository.save(pedido);

        return new CheckoutResponseDTO(checkoutUrl, checkoutId);
    }

    private String criarProdutoTemporario(Long pedidoId, long valorCentavos, String externalId) {
        ObjectNode produto = objectMapper.createObjectNode();
        produto.put("externalId", externalId);
        produto.put("name", "Pedido #" + pedidoId + " - ERP Corporativo");
        produto.put("description", "Pagamento do pedido #" + pedidoId);
        produto.put("price", valorCentavos);
        produto.put("currency", "BRL");
        // sem cycle = produto avulso (ONE_TIME)

        JsonNode resposta = abacatePayApiService.criarProduto(produto);
        String prodId = resposta.path("data").path("id").asText(null);
        if (prodId == null || prodId.isBlank()) {
            // tenta direto no nó raiz (caso a API retorne data flat)
            prodId = resposta.path("id").asText(null);
        }
        if (prodId == null || prodId.isBlank()) {
            throw new BusinessException("AbacatePay não retornou ID do produto criado.");
        }
        return prodId;
    }

    private ObjectNode montarCorpoCheckout(String produtoId, String externalId, Long pedidoId) {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("id", produtoId);
        item.put("quantity", 1);

        root.put("externalId", externalId);
        root.put("returnUrl", frontendUrl + "/pedidos?ap=failure");
        root.put("completionUrl", frontendUrl + "/pedidos?ap=success");

        ArrayNode methods = root.putArray("methods");
        methods.add("PIX");
        methods.add("CARD");

        ObjectNode metadata = root.putObject("metadata");
        metadata.put("tipo", "pedido");
        metadata.put("pedido_id", pedidoId);

        return root;
    }
}
