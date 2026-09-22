package com.votacao.application.model;

public record Voto(
        Long id,
        Sessao sessao,
        String documento,
        Escolha escolhaVoto
) {

    public enum Escolha {
        SIM,
        NAO
    }

}
