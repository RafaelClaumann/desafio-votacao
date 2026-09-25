package com.votacao.application.model.exception;

/**
 * Exceção lançada quando a sessão ainda está aberta em uma operação que exige fechamento.
 */
public class SessaoIsOpenException extends RuntimeException {

    /**
     * Cria a exceção com o identificador da sessão ainda aberta.
     *
     * @param id identificador da sessão
     */
    public SessaoIsOpenException(Long id) {
        super("A sessão " + id + " ainda está aberta");
    }

}
