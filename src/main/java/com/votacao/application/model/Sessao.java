package com.votacao.application.model;

import java.time.LocalDateTime;

public record Sessao(
        Long id,
        Pauta pauta,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {

    public boolean isOpen(LocalDateTime now) {
        return expiresAt != null && now.isBefore(expiresAt);
    }

}
