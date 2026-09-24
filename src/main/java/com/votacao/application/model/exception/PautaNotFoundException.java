package com.votacao.application.model.exception;

public class PautaNotFoundException extends RuntimeException {

    public PautaNotFoundException(Long id) {
        super("Pauta com id " + id + " não encontrada");
    }

}
