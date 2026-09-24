package com.votacao.entrypoint.client.documentvalidator;

import com.votacao.entrypoint.client.exception.HttpIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("DocumentoValidatorClient")
class DocumentoValidatorClientTest {

    private static final String STATUS_ENDPOINT = "https://httpbin.org/status/200,400,404,500";
    private static final String ANYTHING_ENDPOINT = "https://httpbin.org/anything";

    private MockRestServiceServer server;
    private DocumentoValidatorClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DocumentoValidatorClient(builder.baseUrl("https://httpbin.org").build());
    }

    @ParameterizedTest(name = "status {0} should make the documento {1}")
    @CsvSource({
            "BAD_REQUEST, REJECTED",
            "NOT_FOUND, REJECTED"
    })
    @DisplayName("should reject the documento when the external service responds 4xx")
    void isValidDocumento_shouldReject_whenExternalServiceResponds4xx(
            HttpStatus status,
            String expectedOutcome
    ) {
        server.expect(requestTo(STATUS_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(status));

        boolean isValid = client.isValidDocumento("12345678909");

        if ("VALID".equals(expectedOutcome)) {
            assertTrue(isValid);
        } else {
            assertFalse(isValid);
        }
        server.verify();
    }

    @Test
    @DisplayName("should return true when the external service returns 200 and echoes the documento")
    void isValidDocumento_shouldReturnTrue_whenExternalServiceEchoesDocumento() {
        server.expect(requestTo(STATUS_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo(ANYTHING_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"json\": {\"documento\": \"12345678909\"}}",
                        MediaType.APPLICATION_JSON
                ));

        assertTrue(client.isValidDocumento("12345678909"));
        server.verify();
    }

    @Test
    @DisplayName("should throw HttpIntegrationException when the external service responds 5xx")
    void isValidDocumento_shouldThrowHttpIntegrationException_whenExternalServiceResponds500() {
        server.expect(requestTo(STATUS_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(HttpIntegrationException.class, () -> client.isValidDocumento("12345678909"));
        server.verify();
    }

    @Test
    @DisplayName("should throw HttpIntegrationException when the external service is unreachable")
    void isValidDocumento_shouldThrowHttpIntegrationException_whenExternalServiceIsUnreachable() {
        server.expect(requestTo(STATUS_ENDPOINT))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withException(new IOException("connection refused")));

        assertThrows(HttpIntegrationException.class, () -> client.isValidDocumento("12345678909"));
        server.verify();
    }

}
