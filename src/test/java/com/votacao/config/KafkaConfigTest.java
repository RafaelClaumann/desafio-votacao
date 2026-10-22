package com.votacao.config;

import com.votacao.entrypoint.kafka.VotoEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DisplayName("KafkaConfig")
class KafkaConfigTest {

    @Autowired
    private ProducerFactory<String, VotoEvent> producerFactory;

    @Test
    @DisplayName("Should bound the producer blocking time so the request thread is not held for 60 seconds")
    void producerFactory_shouldBoundTheProducerBlockingTime_whenBrokerIsUnreachable() {
        Map<String, Object> configuration = producerFactory.getConfigurationProperties();

        assertAll(
                () -> assertEquals("5000", configuration.get("max.block.ms")),
                () -> assertEquals("10000", configuration.get("request.timeout.ms")),
                () -> assertEquals("30000", configuration.get("delivery.timeout.ms"))
        );
    }

    @Test
    @DisplayName("Should keep the idempotent and acknowledged configuration of the producer")
    void producerFactory_shouldKeepTheIdempotentConfiguration_whenProducerIsCreated() {
        Map<String, Object> configuration = producerFactory.getConfigurationProperties();

        assertAll(
                () -> assertEquals("true", configuration.get("enable.idempotence")),
                () -> assertEquals("all", configuration.get("acks"))
        );
    }

}
