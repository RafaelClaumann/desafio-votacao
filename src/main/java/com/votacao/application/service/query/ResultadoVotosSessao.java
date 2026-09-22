package com.votacao.application.service.query;

public record ResultadoVotosSessao(
        long idSessao,
        long totalVotosSim,
        long totalVotosNao
) {

    public long totalVotos() {
        return totalVotosSim + totalVotosNao;
    }

    public boolean aprovada() {
        return totalVotosSim > totalVotosNao;
    }

}
