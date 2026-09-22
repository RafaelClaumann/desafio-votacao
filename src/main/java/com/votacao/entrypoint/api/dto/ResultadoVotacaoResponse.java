package com.votacao.entrypoint.api.dto;

import com.votacao.application.service.query.ResultadoVotosSessao;

public record ResultadoVotacaoResponse(Long idSessao, long votosSim, long votosNao, long total, boolean aprovada) {

    public static ResultadoVotacaoResponse from(ResultadoVotosSessao resultado) {
        return new ResultadoVotacaoResponse(
                resultado.idSessao(),
                resultado.totalVotosSim(),
                resultado.totalVotosNao(),
                resultado.totalVotos(),
                resultado.aprovada()
        );
    }

}
