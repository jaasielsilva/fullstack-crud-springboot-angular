package com.clientes_api.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Formato: EMPRESA_{empresaId}_PLANO_{planoId}_ASSINATURA_{assinaturaId}
 */
public record MercadoPagoExternalReference(long empresaId, long planoId, long assinaturaId) {

    private static final Pattern PATTERN = Pattern.compile(
            "^EMPRESA_(\\d+)_PLANO_(\\d+)_ASSINATURA_(\\d+)(?:_.*)?$"
    );

    public static Optional<MercadoPagoExternalReference> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Matcher m = PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(new MercadoPagoExternalReference(
                Long.parseLong(m.group(1)),
                Long.parseLong(m.group(2)),
                Long.parseLong(m.group(3))
        ));
    }

    public static String format(long empresaId, long planoId, long assinaturaId) {
        return "EMPRESA_" + empresaId + "_PLANO_" + planoId + "_ASSINATURA_" + assinaturaId;
    }
}
