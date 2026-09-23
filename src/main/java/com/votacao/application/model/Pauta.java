package com.votacao.application.model;

/**
 * Representa uma pauta de votação.
 *
 * @param id identificador da pauta
 * @param titulo título da pauta, normalizado em caso de espaços em excesso
 * @param tempoVotacaoMinutos duração da sessão em minutos
 */
public record Pauta(Long id, String titulo, Long tempoVotacaoMinutos) {

    /**
     * Cria uma pauta e remove espaços em branco no início e no fim do título.
     */
    public Pauta {
        titulo = titulo == null ? null : titulo.trim();
    }

}
