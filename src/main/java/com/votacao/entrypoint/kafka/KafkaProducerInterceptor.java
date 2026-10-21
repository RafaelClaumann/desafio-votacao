package com.votacao.entrypoint.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.votacao.entrypoint.filter.MDCRequestFilter.CORRELATION_ID_HEADER;
import static com.votacao.entrypoint.filter.MDCRequestFilter.MDC_CORRELATION_ID_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class KafkaProducerInterceptor implements ProducerInterceptor<String, VotoEvent> {

    @Override
    public ProducerRecord<String, VotoEvent> onSend(ProducerRecord<String, VotoEvent> record) {
        record.headers().add(new RecordHeader(CORRELATION_ID_HEADER, MDC.get(MDC_CORRELATION_ID_KEY).getBytes(UTF_8)));
        return record;
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

}
