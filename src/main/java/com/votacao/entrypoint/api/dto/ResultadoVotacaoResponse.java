package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.StatusVotacao;

/**
 * Resposta HTTP com a apuração final de uma sessão.
 *
 * @param idSessao identificador da sessão apurada
 * @param votosSim total de votos favoráveis
 * @param votosNao total de votos contrários
 * @param total total de votos registrados
 * @param status status final da votação
 */
public record ResultadoVotacaoResponse(
        Long idSessao,
        long votosSim,
        long votosNao,
        long total,
        StatusVotacao status
) {
}
