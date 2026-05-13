package com.clientes_api.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clientes_api.config.TenantContext;
import com.clientes_api.dto.CheckoutResponseDTO;
import com.clientes_api.dto.ItemPedidoRequestDTO;
import com.clientes_api.dto.ItemPedidoResponseDTO;
import com.clientes_api.dto.PedidoRequestDTO;
import com.clientes_api.dto.PedidoResponseDTO;
import com.clientes_api.exception.BusinessException;
import com.clientes_api.exception.ResourceNotFoundException;
import com.clientes_api.model.Cliente;
import com.clientes_api.model.ItemPedido;
import com.clientes_api.model.Pagamento;
import com.clientes_api.model.Pedido;
import com.clientes_api.model.Produto;
import com.clientes_api.model.StatusPedido;
import com.clientes_api.model.enums.StatusPagamento;
import com.clientes_api.repository.ClienteRepository;
import com.clientes_api.repository.PedidoRepository;
import com.clientes_api.repository.ProdutoRepository;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final CheckoutPedidoMercadoPagoService checkoutPedidoMercadoPagoService;
    private final PagamentoService pagamentoService;

    @Value("${app.pedido-simular-pagamento:false}")
    private boolean pedidoSimularPagamentoEnabled;

    private long requireTenantId() {
        Long t = TenantContext.getCurrentTenant();
        if (t == null || t == 0L) {
            throw new BusinessException("Contexto de empresa não disponível.");
        }
        return t;
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public PedidoResponseDTO criarPedido(PedidoRequestDTO request) {
        Pedido pedido = new Pedido();
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setStatus(StatusPedido.ABERTO);
        preencherDadosPedido(pedido, request);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        return converterParaResponse(pedidoSalvo);
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public PedidoResponseDTO atualizarPedido(Long id, PedidoRequestDTO request) {
        long tenantId = requireTenantId();
        Pedido pedido = pedidoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new BusinessException("Só é possível alterar pedidos em aberto.");
        }

        devolverEstoqueItens(pedido.getItens());
        preencherDadosPedido(pedido, request);

        Pedido pedidoAtualizado = pedidoRepository.save(pedido);
        return converterParaResponse(pedidoAtualizado);
    }

    public List<PedidoResponseDTO> listarPedidos() {
        long tenantId = requireTenantId();
        return pedidoRepository.findAllByTenantIdOrderByDataPedidoDesc(tenantId)
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public PedidoResponseDTO buscarPorId(Long id) {
        long tenantId = requireTenantId();
        Pedido pedido = pedidoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        return converterParaResponse(pedido);
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public void deletarPedido(Long id) {
        long tenantId = requireTenantId();
        Pedido pedido = pedidoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new BusinessException("Só é possível excluir pedidos em aberto.");
        }

        devolverEstoqueItens(pedido.getItens());
        pedidoRepository.delete(pedido);
    }

    public CheckoutResponseDTO iniciarCheckoutPedido(Long id) {
        return checkoutPedidoMercadoPagoService.criarCheckoutPedido(id, requireTenantId());
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public PedidoResponseDTO marcarEntregue(Long id) {
        long tenantId = requireTenantId();
        Pedido pedido = pedidoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.PAGO) {
            throw new BusinessException("Só é possível marcar como entregue pedidos pagos.");
        }

        pedido.setStatus(StatusPedido.ENTREGUE);
        return converterParaResponse(pedidoRepository.save(pedido));
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public PedidoResponseDTO cancelarPedido(Long id) {
        long tenantId = requireTenantId();
        Pedido pedido = pedidoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        if (pedido.getStatus() == StatusPedido.PAGO || pedido.getStatus() == StatusPedido.ENTREGUE) {
            throw new BusinessException("Pedido pago ou entregue: cancelamento deve passar pelo financeiro/estorno.");
        }
        if (pedido.getStatus() == StatusPedido.CANCELADO) {
            throw new BusinessException("Pedido já está cancelado.");
        }

        devolverEstoqueItens(pedido.getItens());
        pedido.setStatus(StatusPedido.CANCELADO);
        return converterParaResponse(pedidoRepository.save(pedido));
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public PedidoResponseDTO simularPagamentoManual(Long id) {
        if (!pedidoSimularPagamentoEnabled) {
            throw new BusinessException("Simulação de pagamento de pedido está desativada neste ambiente.");
        }

        long tenantId = requireTenantId();
        Pedido pedido = pedidoRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new BusinessException("Só é possível simular pagamento em pedidos em aberto.");
        }

        pedido.setStatus(StatusPedido.PAGO);
        pedido.setDataPagamento(LocalDateTime.now());
        return converterParaResponse(pedidoRepository.save(pedido));
    }

    /**
     * Chamado pelo webhook do Mercado Pago após {@link TenantContext} já estar setado para o tenant do pedido.
     */
    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public void processarWebhookMercadoPagoPedido(long pedidoId,
                                                  String mercadoPagoPaymentId,
                                                  JsonNode payment,
                                                  String rawPayment,
                                                  String externalRef,
                                                  StatusPagamento statusPagamento) {
        long tenantId = requireTenantId();

        Pedido pedido = pedidoRepository.findByIdAndTenantId(pedidoId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado para external_reference"));

        Pagamento pagamento = pagamentoService.buscarPorMercadoPagoId(mercadoPagoPaymentId)
                .orElseGet(Pagamento::new);
        pagamento.setTenantId(tenantId);
        pagamento.setPedido(pedido);
        pagamento.setAssinatura(null);
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

        String mpStatus = payment.path("status").asText("unknown");
        if ("approved".equalsIgnoreCase(mpStatus)) {
            if (pedido.getStatus() == StatusPedido.ABERTO) {
                pedido.setStatus(StatusPedido.PAGO);
                pedido.setDataPagamento(LocalDateTime.now());
                pedidoRepository.save(pedido);
            }
        }
    }

    /**
     * Com {@code orphanRemoval = true}, não substituir a coleção {@code pedido.itens} por uma nova lista:
     * mutar a instância existente (clear + add) para o Hibernate remover órfãos corretamente.
     */
    private void preencherDadosPedido(Pedido pedido, PedidoRequestDTO request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new BusinessException("O pedido deve ter pelo menos um item.");
        }

        long tenantId = requireTenantId();

        Cliente cliente = clienteRepository.findByIdAndTenantId(request.getClienteId(), tenantId)
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        pedido.setCliente(cliente);

        List<ItemPedido> destinoItens = pedido.getItens();
        if (destinoItens == null) {
            destinoItens = new ArrayList<>();
            pedido.setItens(destinoItens);
        } else {
            destinoItens.clear();
        }

        double valorTotal = 0.0;

        for (ItemPedidoRequestDTO itemRequest : request.getItens()) {
            if (itemRequest.getQuantidade() == null || itemRequest.getQuantidade() <= 0) {
                throw new BusinessException("Cada item deve ter quantidade maior que zero.");
            }

            Produto produto = produtoRepository.findByIdAndTenantId(itemRequest.getProdutoId(), tenantId)
                    .orElseThrow(() -> new BusinessException("Um dos produtos informados não foi encontrado."));

            int estoqueAtual = produto.getQuantidade() != null ? produto.getQuantidade() : 0;
            int quantidadeSolicitada = itemRequest.getQuantidade();
            if (quantidadeSolicitada > estoqueAtual) {
                throw new BusinessException(
                        "Estoque insuficiente para o produto \"" + produto.getNome() + "\" (disponível: " + estoqueAtual + ").");
            }

            produto.setQuantidade(estoqueAtual - quantidadeSolicitada);
            double valorUnitario = produto.getPreco() != null ? produto.getPreco() : 0.0;
            double subtotal = valorUnitario * quantidadeSolicitada;

            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setProduto(produto);
            item.setQuantidade(quantidadeSolicitada);
            item.setValorUnitario(valorUnitario);
            item.setSubtotal(subtotal);

            destinoItens.add(item);
            valorTotal += subtotal;
        }

        pedido.setValorTotal(valorTotal);
    }

    private void devolverEstoqueItens(List<ItemPedido> itens) {
        if (itens == null) {
            return;
        }

        for (ItemPedido item : itens) {
            if (item.getProduto() == null) {
                continue;
            }
            Produto produto = item.getProduto();
            int estoqueAtual = produto.getQuantidade() != null ? produto.getQuantidade() : 0;
            int quantidadeItem = item.getQuantidade() != null ? item.getQuantidade() : 0;
            produto.setQuantidade(estoqueAtual + quantidadeItem);
        }
    }

    private PedidoResponseDTO converterParaResponse(Pedido pedido) {
        PedidoResponseDTO response = new PedidoResponseDTO();
        response.setId(pedido.getId());
        response.setClienteId(pedido.getCliente().getId());
        response.setCliente(pedido.getCliente().getNome());
        response.setStatus(pedido.getStatus().name());
        response.setValorTotal(pedido.getValorTotal());
        response.setDataPedido(pedido.getDataPedido());
        response.setDataPagamento(pedido.getDataPagamento());
        response.setMercadoPagoPreferenceId(pedido.getMercadoPagoPreferenceId());

        List<ItemPedidoResponseDTO> itensResponse = pedido.getItens() == null
                ? List.of()
                : pedido.getItens()
                .stream()
                .map(item -> {
                    ItemPedidoResponseDTO itemResponse = new ItemPedidoResponseDTO();
                    itemResponse.setProdutoId(item.getProduto().getId());
                    itemResponse.setProduto(item.getProduto().getNome());
                    itemResponse.setQuantidade(item.getQuantidade());
                    itemResponse.setValorUnitario(item.getValorUnitario());
                    itemResponse.setSubtotal(item.getSubtotal());
                    return itemResponse;
                })
                .toList();

        response.setItens(itensResponse);

        return response;
    }
}
