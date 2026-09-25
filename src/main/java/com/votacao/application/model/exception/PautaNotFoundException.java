package com.votacao.application.model.exception;

/**
 * Exceção lançada quando uma pauta não existe.
 */
public class PautaNotFoundException extends RuntimeException {

    /**
     * Cria a exceção com o identificador da pauta inexistente.
     *
     * @param id identificador da pauta
     */
    public PautaNotFoundException(Long id) {
        super("Pauta com id " + id + " não encontrada");
    }

}
