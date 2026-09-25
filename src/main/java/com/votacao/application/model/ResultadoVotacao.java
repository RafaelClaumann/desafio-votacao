package com.votacao.application.model;

/**
 * Resultado resumido de uma sessão de votação.
 *
 * @param totalVotosSim quantidade de votos favoráveis
 * @param totalVotosNao quantidade de votos contrários
 */
public record ResultadoVotacao(long totalVotosSim, long totalVotosNao) {

    /**
     * Calcula o total de votos registrados na sessão.
     *
     * @return soma dos votos sim e não
     */
    public long totalVotos() {
        return totalVotosSim + totalVotosNao;
    }

    /**
     * Determina o status final da votação.
     *
     * @return enum representando a situação da pauta
     */
    public StatusVotacao status() {
        if (totalVotos() == 0) {
            return StatusVotacao.SEM_VOTOS;
        }

        if (totalVotosSim > totalVotosNao) return StatusVotacao.APROVADA;
        if (totalVotosSim < totalVotosNao) return StatusVotacao.REJEITADA;
        return StatusVotacao.EMPATE;
    }

}
