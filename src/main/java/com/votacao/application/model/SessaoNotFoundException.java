package com.votacao.application.model;

public class SessaoNotFoundException extends RuntimeException {

    public SessaoNotFoundException(Long id) {
        super("Sessão not found with id: " + id);
    }

}
