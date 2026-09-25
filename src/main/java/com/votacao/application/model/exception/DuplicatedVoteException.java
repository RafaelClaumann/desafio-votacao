package com.votacao.application.model.exception;

/**
 * Exceção lançada quando o mesmo documento tenta votar mais de uma vez na mesma sessão.
 */
public class DuplicatedVoteException extends RuntimeException {

    /**
     * Cria a exceção com o identificador da sessão e o documento duplicado.
     *
     * @param idSessao identificador da sessão
     * @param documento documento duplicado
     */
    public DuplicatedVoteException(Long idSessao, String documento) {
        super("O documento " + documento + " já votou na sessão " + idSessao);
    }

}
