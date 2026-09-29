package com.votacao.application.service.query;

import com.votacao.application.model.ResultadoVotacao;

/**
 * Resultado de apuração de uma sessão de votação.
 *
 * @param idSessao identificador da sessão
 * @param resultado valores da apuração
 */
public record ApuracaoSessao(
        long idSessao,
        ResultadoVotacao resultado
) {

}
