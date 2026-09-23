package com.votacao.entrypoint.api.handler;

import java.time.Instant;
import java.util.List;

/**
 * Estrutura padronizada para respostas de erro da API.
 *
 * @param timestamp momento em que o erro ocorreu
 * @param status código HTTP do erro
 * @param error descrição do status HTTP
 * @param message mensagem detalhada do erro
 * @param path caminho da requisição que gerou o problema
 * @param fieldErrors erros de validação por campo
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    /**
     * Detalhamento de um erro de validação por campo.
     *
     * @param field nome do campo com falha
     * @param message descrição do problema
     */
    public record FieldError(String field, String message) {
    }
}
