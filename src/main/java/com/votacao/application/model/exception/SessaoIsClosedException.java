package com.votacao.application.model.exception;

public class SessaoIsClosedException extends RuntimeException {

    public SessaoIsClosedException(Long id) {
        super("A sessão " + id + " está fechada");
    }

}
