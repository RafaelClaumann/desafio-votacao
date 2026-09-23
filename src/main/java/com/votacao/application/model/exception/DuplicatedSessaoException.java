package com.votacao.application.model.exception;

public class DuplicatedSessaoException extends RuntimeException {

    public DuplicatedSessaoException(Long pautaId) {
        super("Já existe uma sessão para a pauta: " + pautaId);
    }

}