package com.votacao.application.model;

public class DuplicatedVoteException extends RuntimeException {

    public DuplicatedVoteException(Long idSessao, String documento) {
        super("Duplicated vote for document: " + documento + " in session with id: " + idSessao);
    }

}
