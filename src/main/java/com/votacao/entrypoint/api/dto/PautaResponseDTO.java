package com.votacao.entrypoint.api.dto;

import java.time.LocalDateTime;

public record PautaResponseDTO(
        long id,
        String titulo,
        long tempoVotacaoMinutos,
        LocalDateTime createdAt
) {
}
