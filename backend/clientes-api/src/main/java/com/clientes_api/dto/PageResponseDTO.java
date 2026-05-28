package com.clientes_api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope padrão de paginação da API (page 0-based).
 */
public record PageResponseDTO<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}
