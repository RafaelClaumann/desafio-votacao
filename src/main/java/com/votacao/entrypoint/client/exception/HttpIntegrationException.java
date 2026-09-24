package com.votacao.entrypoint.client.exception;

import org.springframework.http.HttpStatusCode;

public class HttpIntegrationException extends RuntimeException {

    public HttpIntegrationException(HttpStatusCode httpStatus, String message) {
        super("Status: " + httpStatus + ", message: " + message);
    }
}
