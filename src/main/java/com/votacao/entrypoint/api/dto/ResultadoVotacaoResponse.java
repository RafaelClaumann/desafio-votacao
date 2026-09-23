package com.votacao.entrypoint.api.dto;

import com.votacao.application.service.query.ApuracaoSessao;

public record ResultadoVotacaoResponse(Long idSessao, long votosSim, long votosNao, long total, boolean aprovada) {

    public static ResultadoVotacaoResponse fromDomain(ApuracaoSessao resultado) {
        return new ResultadoVotacaoResponse(
                resultado.idSessao(),
                resultado.resultado().totalVotosSim(),
                resultado.resultado().totalVotosNao(),
                resultado.resultado().totalVotos(),
                resultado.resultado().aprovada()
        );
    }

}
