package com.votacao.entrypoint.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.votacao.entrypoint.filter.MDCRequestFilter.CORRELATION_ID_HEADER;
import static com.votacao.entrypoint.filter.MDCRequestFilter.MDC_CORRELATION_ID_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class KafkaProducerInterceptor implements ProducerInterceptor<String, VotoEvent> {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerInterceptor.class);

    @Override
    public ProducerRecord<String, VotoEvent> onSend(ProducerRecord<String, VotoEvent> record) {
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            log.warn("Mensagem publicada sem identificador de correlação: o header {} não será anexado", CORRELATION_ID_HEADER);
            return record;
        }

        record.headers().add(new RecordHeader(CORRELATION_ID_HEADER, correlationId.getBytes(UTF_8)));
        return record;
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

}
