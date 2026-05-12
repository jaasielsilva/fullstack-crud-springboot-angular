package com.clientes_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clientes_api.dto.CheckoutResponseDTO;
import com.clientes_api.dto.PedidoRequestDTO;
import com.clientes_api.dto.PedidoResponseDTO;
import com.clientes_api.service.PedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public PedidoResponseDTO criarPedido(@Valid @RequestBody PedidoRequestDTO request) {
        return pedidoService.criarPedido(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public PedidoResponseDTO atualizarPedido(@PathVariable Long id, @Valid @RequestBody PedidoRequestDTO request) {
        return pedidoService.atualizarPedido(id, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public List<PedidoResponseDTO> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public PedidoResponseDTO buscarPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public void deletarPedido(@PathVariable Long id) {
        pedidoService.deletarPedido(id);
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public CheckoutResponseDTO checkout(@PathVariable Long id) {
        return pedidoService.iniciarCheckoutPedido(id);
    }

    @PostMapping("/{id}/marcar-entregue")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public PedidoResponseDTO marcarEntregue(@PathVariable Long id) {
        return pedidoService.marcarEntregue(id);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'VENDEDOR', 'SUPORTE')")
    public PedidoResponseDTO cancelar(@PathVariable Long id) {
        return pedidoService.cancelarPedido(id);
    }

    @PostMapping("/{id}/simular-pagamento")
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public PedidoResponseDTO simularPagamento(@PathVariable Long id) {
        return pedidoService.simularPagamentoManual(id);
    }
}
