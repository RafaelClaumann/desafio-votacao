package com.votacao.entrypoint.client.documentvalidator;

import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.config.DocumentoValidatorProperties;
import com.votacao.entrypoint.client.exception.HttpIntegrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final boolean isValidationEnabled;

    public DocumentoValidatorClient(
            RestClient documentoValidatorRestClient,
            DocumentoValidatorProperties properties
    ) {
        this.restClient = documentoValidatorRestClient;
        this.isValidationEnabled = properties.isEnabled();
    }

    @Override
    public boolean isValidDocumento(String documento) {
        if (!isValidationEnabled) {
            log.debug("Validação externa de documento desabilitada: documento aceito sem consultar o serviço externo");
            return true;
        }

        try {
            simularStatusDoServicoExterno();

            ResponseEntity<ValidatorResponse> response = restClient.post()
                    .uri("/anything")
                    .body(Map.of("documento", documento))
                    .retrieve()
                    .toEntity(ValidatorResponse.class);

            log.info("Response from Documento Validator: status={}, body={}", response.getStatusCode(), response.getBody());

        } catch (HttpClientErrorException ex) {
            return false;
        } catch (HttpServerErrorException ex) {
            throw new HttpIntegrationException(ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            throw new HttpIntegrationException(HttpStatus.SERVICE_UNAVAILABLE);
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
