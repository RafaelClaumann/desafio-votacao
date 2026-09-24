package com.votacao.application.model.exception;

public class SessaoNotFoundException extends RuntimeException {

    public SessaoNotFoundException(Long id) {
        super("Sessão com id " + id + " não encontrada");
    }

}
