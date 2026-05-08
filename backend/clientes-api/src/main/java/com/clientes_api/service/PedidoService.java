package com.clientes_api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clientes_api.dto.ItemPedidoRequestDTO;
import com.clientes_api.dto.ItemPedidoResponseDTO;
import com.clientes_api.dto.PedidoRequestDTO;
import com.clientes_api.dto.PedidoResponseDTO;
import com.clientes_api.model.Cliente;
import com.clientes_api.model.ItemPedido;
import com.clientes_api.model.Pedido;
import com.clientes_api.model.Produto;
import com.clientes_api.model.StatusPedido;
import com.clientes_api.repository.ClienteRepository;
import com.clientes_api.repository.PedidoRepository;
import com.clientes_api.repository.ProdutoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;

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
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        devolverEstoqueItens(pedido.getItens());
        preencherDadosPedido(pedido, request);

        Pedido pedidoAtualizado = pedidoRepository.save(pedido);
        return converterParaResponse(pedidoAtualizado);
    }

    public List<PedidoResponseDTO> listarPedidos() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::converterParaResponse)
                .toList();
    }

    public PedidoResponseDTO buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        return converterParaResponse(pedido);
    }

    @CacheEvict(value = "dashboardExecutivo", allEntries = true)
    public void deletarPedido(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        devolverEstoqueItens(pedido.getItens());
        pedidoRepository.delete(pedido);
    }

    private void preencherDadosPedido(Pedido pedido, PedidoRequestDTO request) {
        if (request.getItens() == null || request.getItens().isEmpty()) {
            throw new RuntimeException("Pedido deve ter pelo menos 1 item");
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente informado não existe"));

        pedido.setCliente(cliente);

        List<ItemPedido> itens = new ArrayList<>();
        double valorTotal = 0.0;

        for (ItemPedidoRequestDTO itemRequest : request.getItens()) {
            if (itemRequest.getQuantidade() == null || itemRequest.getQuantidade() <= 0) {
                throw new RuntimeException("Quantidade do item deve ser maior que zero");
            }

            Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

            int estoqueAtual = produto.getQuantidade() != null ? produto.getQuantidade() : 0;
            int quantidadeSolicitada = itemRequest.getQuantidade();
            if (quantidadeSolicitada > estoqueAtual) {
                throw new RuntimeException(
                        "Produto sem estoque disponível"
                );
            }

            produto.setQuantidade(estoqueAtual - quantidadeSolicitada);
            double valorUnitario = produto.getPreco();
            double subtotal = valorUnitario * quantidadeSolicitada;

            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setProduto(produto);
            item.setQuantidade(quantidadeSolicitada);
            item.setValorUnitario(valorUnitario);
            item.setSubtotal(subtotal);

            itens.add(item);
            valorTotal += subtotal;
        }

        pedido.setItens(itens);
        pedido.setValorTotal(valorTotal);
    }

    private void devolverEstoqueItens(List<ItemPedido> itens) {
        if (itens == null) {
            return;
        }

        for (ItemPedido item : itens) {
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

        List<ItemPedidoResponseDTO> itensResponse = pedido.getItens()
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
