package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.StatusVotacao;

public record ResultadoVotacaoResponse(
        long idSessao,
        long votosSim,
        long votosNao,
        long total,
        StatusVotacao status
) {
}
