package com.clientes_api.service;

import com.clientes_api.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ajusta o {@code unit_price} enviado na preferência Mercado Pago (ex.: valor mínimo para teste).
 */
@Component
public class MercadoPagoValorPreferenciaService {

    @Value("${mercadopago.checkout-valor-override:}")
    private String checkoutValorOverride;

    /**
     * Valor efetivo de {@code items[].unit_price}. Se {@code mercadopago.checkout-valor-override}
     * estiver definido (ex.: 1.00), substitui o valor real do plano ou do pedido.
     */
    public BigDecimal resolverPrecoUnitario(BigDecimal valorReal) {
        BigDecimal base = valorReal.setScale(2, RoundingMode.HALF_UP);
        if (checkoutValorOverride == null || checkoutValorOverride.isBlank()) {
            return base;
        }
        try {
            return new BigDecimal(checkoutValorOverride.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new BusinessException(
                    "Configuração inválida: mercadopago.checkout-valor-override deve ser um número decimal.");
        }
    }
}
