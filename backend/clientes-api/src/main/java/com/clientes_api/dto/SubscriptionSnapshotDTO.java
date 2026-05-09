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
        List<String> recursosLiberados
) {}
