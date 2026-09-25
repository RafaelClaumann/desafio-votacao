package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Voto;

import java.time.LocalDateTime;

public record VotoResponseDTO(
        long id,
        long idSessao,
        long idPauta,
        String tituloPauta,
        String documento,
        Voto.Escolha escolhaVoto,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {
}
