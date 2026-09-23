package com.votacao.application.service.query;

import com.votacao.application.model.ResultadoVotacao;

public record ApuracaoSessao(
        long idSessao,
        ResultadoVotacao resultado
) {

}
