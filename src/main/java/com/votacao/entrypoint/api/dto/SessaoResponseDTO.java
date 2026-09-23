package com.votacao.entrypoint.api.dto;

import java.time.LocalDateTime;

public record SessaoResponseDTO(
        Long idSessao,
        Long idPauta,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        boolean isOpen
) {
}
