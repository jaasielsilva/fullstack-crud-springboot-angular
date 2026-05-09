package com.clientes_api.dto;

public record LoginResponseDTO(String token, SubscriptionSnapshotDTO subscription) {
}
