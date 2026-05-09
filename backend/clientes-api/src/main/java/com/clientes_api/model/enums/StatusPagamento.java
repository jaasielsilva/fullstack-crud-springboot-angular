package com.clientes_api.model.enums;

/**
 * Status persistido localmente após consulta ao Mercado Pago (/v1/payments/{id}).
 */
public enum StatusPagamento {
    APPROVED,
    PENDING,
    IN_PROCESS,
    REJECTED,
    CANCELLED,
    REFUNDED,
    CHARGED_BACK,
    UNKNOWN
}
