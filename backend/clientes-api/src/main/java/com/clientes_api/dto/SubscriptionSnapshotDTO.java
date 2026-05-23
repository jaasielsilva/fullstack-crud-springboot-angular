package com.clientes_api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubscriptionSnapshotDTO(
        String empresaStatus,
        String assinaturaStatus,
        LocalDateTime trialInicio,
        LocalDateTime trialFim,
        Long diasTrialRestantes,
        boolean pagamentoPendente,
        String mensagemPagamentoPendente,
        /** Dias até {@code dataFim} da assinatura ativa (últimos 7 dias do período de 30 dias pós-pagamento). */
        Long diasAteVencimentoAssinatura,
        String mensagemRenovacaoAssinatura,
        List<String> recursosLiberados
) {}
