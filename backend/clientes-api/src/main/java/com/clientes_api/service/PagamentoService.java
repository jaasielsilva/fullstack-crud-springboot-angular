package com.clientes_api.service;

import com.clientes_api.model.Pagamento;
import com.clientes_api.model.enums.StatusPagamento;
import com.clientes_api.repository.PagamentoRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository) {
        this.pagamentoRepository = pagamentoRepository;
    }

    public Pagamento salvar(Pagamento pagamento) {
        return pagamentoRepository.save(pagamento);
    }

    public Optional<Pagamento> buscarPorMercadoPagoId(String mpPaymentId) {
        return pagamentoRepository.findByMercadoPagoPaymentId(mpPaymentId);
    }

    public boolean jaAprovado(String mpPaymentId) {
        return pagamentoRepository.existsByMercadoPagoPaymentIdAndStatus(mpPaymentId, StatusPagamento.APPROVED);
    }
}
