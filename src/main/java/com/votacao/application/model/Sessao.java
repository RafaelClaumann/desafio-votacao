package com.votacao.application.model;

import java.time.LocalDateTime;

public record Sessao(
        Long id,
        Long pautaId,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {
}
