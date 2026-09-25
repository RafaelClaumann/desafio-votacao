package com.votacao.entrypoint.api.dto;

import java.time.LocalDateTime;

public record SessaoResponseDTO(
        long id,
        long idPauta,
        String tituloPauta,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        boolean isOpen
) {
}
