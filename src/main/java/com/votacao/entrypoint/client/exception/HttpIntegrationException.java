package com.votacao.entrypoint.client.exception;

import org.springframework.http.HttpStatusCode;

public class HttpIntegrationException extends RuntimeException {

    public HttpIntegrationException(HttpStatusCode httpStatus) {
        super("Falha na integração externa de validação de documento: " + httpStatus.value());
    }
}
