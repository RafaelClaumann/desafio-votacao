package com.votacao.application.model.exception;

public class InvalidDocumentoException extends RuntimeException {

    public InvalidDocumentoException(String documento) {
        super("Documento inválido: " + documento);
    }

}
