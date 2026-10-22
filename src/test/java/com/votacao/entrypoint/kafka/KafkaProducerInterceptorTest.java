package com.votacao.entrypoint.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static com.votacao.entrypoint.filter.MDCRequestFilter.CORRELATION_ID_HEADER;
import static com.votacao.entrypoint.filter.MDCRequestFilter.MDC_CORRELATION_ID_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("KafkaProducerInterceptor")
class KafkaProducerInterceptorTest {

    private static final String TOPIC = "voto-topic";

    private final KafkaProducerInterceptor interceptor = new KafkaProducerInterceptor();

    @AfterEach
    void cleanupMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should attach the correlation id header when the MDC has the correlation id")
    void onSend_shouldAttachCorrelationIdHeader_whenMdcHasCorrelationId() {
        MDC.put(MDC_CORRELATION_ID_KEY, "correlation-123");

        ProducerRecord<String, VotoEvent> result = interceptor.onSend(record());

        Header header = result.headers().lastHeader(CORRELATION_ID_HEADER);
        assertAll(
                () -> assertNotNull(header),
                () -> assertEquals("correlation-123", new String(header.value(), UTF_8))
        );
    }

    @Test
    @DisplayName("Should not throw and should not attach the header when the MDC has no correlation id")
    void onSend_shouldNotAttachHeaderAndNotThrow_whenMdcHasNoCorrelationId() {
        ProducerRecord<String, VotoEvent> result = assertDoesNotThrow(() -> interceptor.onSend(record()));

        assertNull(result.headers().lastHeader(CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("Should not throw and should not attach the header when the MDC correlation id is blank")
    void onSend_shouldNotAttachHeaderAndNotThrow_whenMdcCorrelationIdIsBlank() {
        MDC.put(MDC_CORRELATION_ID_KEY, "  ");

        ProducerRecord<String, VotoEvent> result = assertDoesNotThrow(() -> interceptor.onSend(record()));

        assertNull(result.headers().lastHeader(CORRELATION_ID_HEADER));
    }

    private ProducerRecord<String, VotoEvent> record() {
        return new ProducerRecord<>(TOPIC, "1", new VotoEvent(1L, "Reforma estatutária", 10L, 1L, "2026-09-23T10:00:00",
                "2026-09-23T10:10:00", 1L, "12345678909", "SIM", "2026-09-23T10:01:00"));
    }

}
