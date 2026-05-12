package com.clientes_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.clientes_api.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByTenantIdOrderByDataPedidoDesc(Long tenantId);

    Optional<Pedido> findByIdAndTenantId(Long id, Long tenantId);
}
