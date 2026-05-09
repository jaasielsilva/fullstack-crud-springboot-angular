package com.clientes_api.repository;

import com.clientes_api.model.Pagamento;
import com.clientes_api.model.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Optional<Pagamento> findByMercadoPagoPaymentId(String mercadoPagoPaymentId);

    boolean existsByMercadoPagoPaymentIdAndStatus(String mercadoPagoPaymentId, StatusPagamento status);
}
