package com.votacao.application.model.exception;

/**
 * Exceção lançada quando uma sessão não existe.
 */
public class SessaoNotFoundException extends RuntimeException {

    /**
     * Cria a exceção com o identificador da sessão inexistente.
     *
     * @param id identificador da sessão
     */
    public SessaoNotFoundException(Long id) {
        super("Sessão com id " + id + " não encontrada");
    }

}
