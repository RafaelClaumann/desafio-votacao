package com.votacao.application.model;

public record ResultadoVotacao(long totalVotosSim, long totalVotosNao) {

    public long totalVotos() {
        return totalVotosSim + totalVotosNao;
    }

    public boolean aprovada() {
        return totalVotosSim > totalVotosNao;
    }

}
