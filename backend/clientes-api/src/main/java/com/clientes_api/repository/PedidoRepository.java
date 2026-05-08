package com.clientes_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.clientes_api.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}