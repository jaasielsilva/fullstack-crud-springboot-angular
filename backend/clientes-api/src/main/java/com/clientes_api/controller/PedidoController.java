package com.clientes_api.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clientes_api.dto.PedidoRequestDTO;
import com.clientes_api.dto.PedidoResponseDTO;
import com.clientes_api.service.PedidoService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponseDTO criarPedido(@Valid @RequestBody PedidoRequestDTO request) {
        return pedidoService.criarPedido(request);
    }

    @PutMapping("/{id}")
    public PedidoResponseDTO atualizarPedido(@PathVariable Long id, @Valid @RequestBody PedidoRequestDTO request) {
        return pedidoService.atualizarPedido(id, request);
    }

    @GetMapping
    public List<PedidoResponseDTO> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @GetMapping("/{id}")
    public PedidoResponseDTO buscarPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarPedido(@PathVariable Long id) {
        pedidoService.deletarPedido(id);
    }
}