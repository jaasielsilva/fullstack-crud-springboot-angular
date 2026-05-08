package com.clientes_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.clientes_api.model.ItemPedido;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {
}