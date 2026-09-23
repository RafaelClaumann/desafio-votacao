package com.votacao.application.model;

/**
 * Representa um voto registrado em uma sessão de votação.
 *
 * @param id identificador do voto
 * @param sessao sessão em que o voto foi registrado
 * @param documento número do documento do eleitor
 * @param escolhaVoto opção escolhida pelo eleitor
 */
public record Voto(
        Long id,
        Sessao sessao,
        String documento,
        Escolha escolhaVoto
) {

    /**
     * Possíveis escolhas aceitas nas votações.
     */
    public enum Escolha {
        SIM,
        NAO
    }

}
