package com.votacao.application.model.exception;

public class DuplicatedVoteException extends RuntimeException {

    public DuplicatedVoteException(Long idSessao, String documento) {
        super("Duplicated vote for document: " + documento + " in session with id: " + idSessao);
    }

}
