package com.votacao.entrypoint.client.documentvalidator;

import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.entrypoint.client.exception.HttpIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
public class DocumentoValidatorClient implements DocumentoValidator {

    private static final Logger log = LoggerFactory.getLogger(DocumentoValidatorClient.class);
    private static final String BASE_URL = "https://httpbin.org";

    private final RestClient restClient;

    public DocumentoValidatorClient() {
        this.restClient = RestClient.create(BASE_URL);
    }

    @Override
    public boolean isValidDocumento(String documento) {
        try {
            this.randomResponseFetcher();

            ValidatorResponse response = restClient.post()
                    .uri("/anything")
                    .body(Map.of("documento", documento))
                    .retrieve()
                    .body(ValidatorResponse.class);

            log.info("Retorno capturado: {}", response);

        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new HttpIntegrationException(ex.getStatusCode(), ex.getMessage());
        } catch (ResourceAccessException ex) {
            throw new HttpIntegrationException(HttpStatus.FAILED_DEPENDENCY, ex.getMessage());
        }

        return true;
    }

    private void randomResponseFetcher() {
        try {
            restClient.get()
                    .uri("/status/200,400,404")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Exception capturada: {}", ex.getMessage());
            throw ex;
        }
    }

}
