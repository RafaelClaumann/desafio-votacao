package com.votacao.config;

import com.votacao.entrypoint.kafka.KafkaProducerInterceptor;
import com.votacao.entrypoint.kafka.VotoEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaProducerInterceptor kafkaProducerInterceptor() {
        return new KafkaProducerInterceptor();
    }

    @Bean
    ProducerFactory<String, VotoEvent> producerFactory(KafkaProperties kafkaProperties, JsonMapper jsonMapper) {
        Map<String, Object> producerProperties = kafkaProperties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(
                producerProperties,
                new StringSerializer(),
                new JacksonJsonSerializer<>(jsonMapper)
        );
    }

    @Bean
    KafkaTemplate<String, VotoEvent> kafkaTemplate(ProducerFactory<String, VotoEvent> votoProducerFactory) {
        KafkaTemplate<String, VotoEvent> stringVotoEventKafkaTemplate = new KafkaTemplate<>(votoProducerFactory);
        stringVotoEventKafkaTemplate.setProducerInterceptor(kafkaProducerInterceptor());

        return stringVotoEventKafkaTemplate;
    }

}
