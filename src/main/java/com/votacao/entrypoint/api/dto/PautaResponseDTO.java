package com.votacao.entrypoint.api.dto;

public record PautaResponseDTO(
        long id,
        String titulo,
        long tempoVotacaoMinutos
) {
}
