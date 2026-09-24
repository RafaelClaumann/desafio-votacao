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

import java.util.Map;

@Component
public class DocumentoValidatorClient implements DocumentoValidator {

    private static final Logger log = LoggerFactory.getLogger(DocumentoValidatorClient.class);

    private final RestClient restClient;

    public DocumentoValidatorClient(RestClient documentoValidatorRestClient) {
        this.restClient = documentoValidatorRestClient;
    }

    @Override
    public boolean isValidDocumento(String documento) {
        try {
            simularStatusDoServicoExterno();

            ValidatorResponse response = restClient.post()
                    .uri("/anything")
                    .body(Map.of("documento", documento))
                    .retrieve()
                    .body(ValidatorResponse.class);

            log.info("Retorno capturado: {}", response);

        } catch (HttpClientErrorException ex) {
            return false;
        } catch (HttpServerErrorException ex) {
            throw new HttpIntegrationException(ex.getStatusCode(), ex.getMessage());
        } catch (ResourceAccessException ex) {
            throw new HttpIntegrationException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        }
        return true;
    }

    private void simularStatusDoServicoExterno() {
        restClient.get()
                .uri("/status/200,400,404,500")
                .retrieve()
                .toBodilessEntity();
    }

}
