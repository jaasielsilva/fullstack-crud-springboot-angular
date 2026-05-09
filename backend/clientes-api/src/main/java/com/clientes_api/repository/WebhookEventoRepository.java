package com.clientes_api.repository;

import com.clientes_api.model.WebhookEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventoRepository extends JpaRepository<WebhookEvento, Long> {

    Optional<WebhookEvento> findByTipoAndMercadoPagoId(String tipo, String mercadoPagoId);
}
