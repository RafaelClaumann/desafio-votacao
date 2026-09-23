package com.votacao.application.model.exception;

/**
 * Exceção lançada quando uma operação é realizada fora do prazo de abertura da sessão.
 */
public class SessaoIsClosedException extends RuntimeException {

    /**
     * Cria a exceção com o identificador da sessão fechada.
     *
     * @param id identificador da sessão
     */
    public SessaoIsClosedException(Long id) {
        super("Sessão with id: " + id + " is closed");
    }

}
