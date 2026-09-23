package com.votacao.application.model;

public record ResultadoVotacao(long totalVotosSim, long totalVotosNao) {

    public long totalVotos() {
        return totalVotosSim + totalVotosNao;
    }

    public StatusVotacao status() {
        if (totalVotosSim > totalVotosNao) return StatusVotacao.APROVADA;
        if (totalVotosSim < totalVotosNao) return StatusVotacao.REJEITADA;
        return StatusVotacao.EMPATE;
    }

}
