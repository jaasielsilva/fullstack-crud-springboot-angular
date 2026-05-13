package com.clientes_api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Chave idempotente para {@link com.clientes_api.model.Pagamento} originado no Abacate Pay,
 * cabendo na coluna {@code mercado_pago_payment_id} (64 caracteres, única).
 */
public final class AbacatePagamentoIds {

    private AbacatePagamentoIds() {
    }

    /**
     * Identificador estável do pagamento Abacate (checkout/cobrança + referência de assinatura).
     */
    public static String chavePagamento(String checkoutId, String externalReference) {
        String a = checkoutId == null ? "" : checkoutId.trim();
        String b = externalReference == null ? "" : externalReference.trim();
        String combined = "abacate|checkout=" + a + "|ref=" + b;
        if (combined.length() <= 64) {
            return combined;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return combined.substring(0, 64);
        }
    }
}
