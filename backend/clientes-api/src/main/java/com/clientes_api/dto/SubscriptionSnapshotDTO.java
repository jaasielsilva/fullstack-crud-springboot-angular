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
        /** Dias até {@code dataFim} da assinatura ativa (alerta de renovação), tipicamente 0–7. */
        Long diasAteVencimentoAssinatura,
        String mensagemRenovacaoAssinatura,
        List<String> recursosLiberados
) {}
