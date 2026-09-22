package com.votacao.application.model.exception;

public class SessaoIsClosedException extends RuntimeException {

    public SessaoIsClosedException(Long id) {
        super("Sessão with id: " + id + " is closed");
    }

}
