package com.votacao.application.model.exception;

public class DuplicatedPautaException extends RuntimeException {

    public DuplicatedPautaException(String titulo) {
        super("Já existe uma pauta com o título: " + titulo);
    }

}
