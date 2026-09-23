package com.votacao.application.model;

public record Pauta(Long id, String titulo, Long tempoVotacaoMinutos) {

    public Pauta {
        titulo = titulo == null ? null : titulo.trim();
    }

}
