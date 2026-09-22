package com.votacao.application.model.exception;

public class PautaNotFoundException extends RuntimeException {

    public PautaNotFoundException(Long id) {
        super("Pauta not found with id: " + id);
    }

}
