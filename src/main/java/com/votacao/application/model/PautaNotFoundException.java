package com.votacao.application.model;

public class PautaNotFoundException extends RuntimeException {

    public PautaNotFoundException(Long id) {
        super("Pauta not found with id: " + id);
    }

}
