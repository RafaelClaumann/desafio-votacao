package com.votacao.application.model.exception;

public class DuplicatedVoteException extends RuntimeException {

    public DuplicatedVoteException(Long idSessao, String documento) {
        super("O documento " + documento + " já votou na sessão " + idSessao);
    }

}
