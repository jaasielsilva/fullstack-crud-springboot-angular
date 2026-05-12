package com.clientes_api.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Referência externa Mercado Pago para pagamento de pedido B2B.
 * Formato: {@code PEDIDO_{tenantId}_{pedidoId}}
 */
public record PedidoMercadoPagoExternalReference(long tenantId, long pedidoId) {

    private static final Pattern PATTERN = Pattern.compile("^PEDIDO_(\\d+)_(\\d+)$");

    public static Optional<PedidoMercadoPagoExternalReference> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Matcher m = PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(new PedidoMercadoPagoExternalReference(
                Long.parseLong(m.group(1)),
                Long.parseLong(m.group(2))));
    }

    public static String format(long tenantId, long pedidoId) {
        return "PEDIDO_" + tenantId + "_" + pedidoId;
    }
}
