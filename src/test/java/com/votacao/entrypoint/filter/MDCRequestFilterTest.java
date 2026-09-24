package com.votacao.entrypoint.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("MDCRequestFilter")
class MDCRequestFilterTest {

    private final MDCRequestFilter filter = new MDCRequestFilter();

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should populate the MDC with the header correlation id when the header is present")
    void doFilter_shouldPopulateMdcWithHeaderCorrelationId_whenHeaderPresent() throws Exception {
        MockHttpServletRequest request = requestWithHeader("correlation-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] capturedCorrelationId = new String[1];
        String[] capturedMethod = new String[1];
        String[] capturedUri = new String[1];

        filter.doFilter(request, response, (req, res) -> {
            capturedCorrelationId[0] = MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY);
            capturedMethod[0] = MDC.get(MDCRequestFilter.MDC_REQUEST_METHOD_KEY);
            capturedUri[0] = MDC.get(MDCRequestFilter.MDC_REQUEST_URI_KEY);
        });

        assertAll(
                () -> assertEquals("correlation-123", capturedCorrelationId[0]),
                () -> assertEquals("POST", capturedMethod[0]),
                () -> assertEquals("/pautas", capturedUri[0]),
                () -> assertNull(MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY))
        );
    }

    @Test
    @DisplayName("Should generate a UUID when the header is absent")
    void doFilter_shouldGenerateUuid_whenHeaderAbsent() throws Exception {
        MockHttpServletRequest request = requestWithHeader(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] capturedCorrelationId = new String[1];

        filter.doFilter(request, response, (req, res) ->
                capturedCorrelationId[0] = MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY));

        assertAll(
                () -> assertNotNull(capturedCorrelationId[0]),
                () -> assertDoesNotThrow(() -> UUID.fromString(capturedCorrelationId[0])),
                () -> assertNull(MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    @DisplayName("Should generate a UUID when the header is blank")
    void doFilter_shouldGenerateUuid_whenHeaderBlank(String header) throws Exception {
        MockHttpServletRequest request = requestWithHeader(header);
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] capturedCorrelationId = new String[1];

        filter.doFilter(request, response, (req, res) ->
                capturedCorrelationId[0] = MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY));

        assertAll(
                () -> assertNotNull(capturedCorrelationId[0]),
                () -> assertDoesNotThrow(() -> UUID.fromString(capturedCorrelationId[0])),
                () -> assertNull(MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY))
        );
    }

    @Test
    @DisplayName("Should clear the MDC when the chain throws")
    void doFilter_shouldClearMdc_whenChainThrows() {
        MockHttpServletRequest request = requestWithHeader("correlation-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(IllegalStateException.class, () -> filter.doFilter(
                request,
                response,
                (req, res) -> {
                    throw new IllegalStateException("boom");
                }
        ));

        assertNull(MDC.get(MDCRequestFilter.MDC_CORRELATION_ID_KEY));
    }

    @Test
    @DisplayName("Should echo the correlation id header on the response when the header is present")
    void doFilter_shouldEchoCorrelationIdHeaderOnResponse_whenHeaderPresent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithHeader("correlation-123"), response, (req, res) -> {
        });

        assertEquals("correlation-123", response.getHeader(MDCRequestFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("Should echo the generated correlation id header on the response when the header is absent")
    void doFilter_shouldEchoCorrelationIdHeaderOnResponse_whenHeaderAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithHeader(null), response, (req, res) -> {
        });

        String echoed = response.getHeader(MDCRequestFilter.CORRELATION_ID_HEADER);
        assertAll(
                () -> assertNotNull(echoed),
                () -> assertDoesNotThrow(() -> UUID.fromString(echoed))
        );
    }

    private MockHttpServletRequest requestWithHeader(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/pautas");
        if (header != null) {
            request.addHeader(MDCRequestFilter.CORRELATION_ID_HEADER, header);
        }
        return request;
    }

}
