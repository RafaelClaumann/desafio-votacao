package com.votacao.application.model;

import java.time.LocalDateTime;

public record Voto(
        Long id,
        Sessao sessao,
        String documento,
        Escolha escolhaVoto,
        LocalDateTime createdAt
) {

    public static Voto registrar(Sessao sessao, String documento, Escolha escolhaVoto) {
        return new Voto(null, sessao, documento, escolhaVoto, null);
    }

    public enum Escolha {
        SIM,
        NAO
    }

}
