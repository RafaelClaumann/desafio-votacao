package com.votacao.entrypoint.api.dto;

public record PautaResponseDTO(
        Long id,
        String titulo,
        Long tempoVotacaoMinutos
) {
}
