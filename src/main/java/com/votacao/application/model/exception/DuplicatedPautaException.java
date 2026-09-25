package com.votacao.application.model.exception;

/**
 * Exceção lançada quando um título de pauta já foi cadastrado.
 */
public class DuplicatedPautaException extends RuntimeException {

    /**
     * Cria a exceção com o título duplicado.
     *
     * @param titulo título que já existe
     */
    public DuplicatedPautaException(String titulo) {
        super("Já existe uma pauta com o título: " + titulo);
    }

}
