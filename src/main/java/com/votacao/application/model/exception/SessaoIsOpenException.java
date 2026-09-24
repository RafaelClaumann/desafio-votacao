package com.votacao.application.model.exception;

public class SessaoIsOpenException extends RuntimeException {

    public SessaoIsOpenException(Long id) {
        super("A sessão " + id + " ainda está aberta");
    }

}
