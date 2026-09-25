package com.votacao.application.model;

public record Voto(
        Long id,
        Sessao sessao,
        String documento,
        Escolha escolhaVoto
) {

    public static Voto registrar(Sessao sessao, String documento, Escolha escolhaVoto) {
        return new Voto(null, sessao, documento, escolhaVoto);
    }

    public enum Escolha {
        SIM,
        NAO
    }

}
