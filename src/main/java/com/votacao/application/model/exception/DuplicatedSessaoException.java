package com.votacao.application.model.exception;

public class DuplicatedSessaoException extends RuntimeException {

    public DuplicatedSessaoException(Long idPauta) {
        super("Já existe uma sessão para a pauta: " + idPauta);
    }

    public DuplicatedSessaoException(Long id, Throwable cause) {
        super("Já existe uma sessão para a pauta: " + id, cause);
    }

}
