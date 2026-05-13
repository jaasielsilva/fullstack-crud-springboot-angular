package com.clientes_api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbacatePagamentoIdsTest {

    @Test
    void chaveCurta_retornaLiteral() {
        String k = AbacatePagamentoIds.chavePagamento("b1", "EMPRESA_1_PLANO_1_ASSINATURA_1");
        assertTrue(k.length() <= 64);
        assertTrue(k.contains("abacate"));
    }

    @Test
    void chaveLonga_retornaHex64() {
        String longId = "bill_" + "x".repeat(80);
        String k = AbacatePagamentoIds.chavePagamento(longId, "EMPRESA_1_PLANO_1_ASSINATURA_1");
        assertEquals(64, k.length());
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            assertTrue((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
        }
    }
}
