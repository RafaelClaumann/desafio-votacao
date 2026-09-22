package com.votacao.application.model.exception;

public class SessaoNotFoundException extends RuntimeException {

    public SessaoNotFoundException(Long id) {
        super("Sessão not found with id: " + id);
    }

}
