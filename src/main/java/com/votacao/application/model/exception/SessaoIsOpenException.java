package com.votacao.application.model.exception;

public class SessaoIsOpenException extends RuntimeException {

    public SessaoIsOpenException(Long id) {
        super("Sessão with id: " + id + " is open");
    }

}
