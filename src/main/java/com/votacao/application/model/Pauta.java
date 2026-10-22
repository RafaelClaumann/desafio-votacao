package com.votacao.application.model;

import java.time.LocalDateTime;

public record Pauta(Long id, String titulo, Long tempoVotacaoMinutos, LocalDateTime createdAt) {

    public Pauta {
        titulo = titulo == null ? null : titulo.trim();
    }

}
