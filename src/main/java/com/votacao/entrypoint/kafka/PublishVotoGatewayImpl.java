package com.votacao.entrypoint.kafka;

import com.votacao.application.gateway.PublishVotoGateway;
import com.votacao.application.service.query.PublishVoto;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublishVotoGatewayImpl implements PublishVotoGateway {

    private static final String TOPIC = "voto-topic";

    private final KafkaTemplate<Long, PublishVoto> kafkaTemplate;

    public PublishVotoGatewayImpl(KafkaTemplate<Long, PublishVoto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishVoto(PublishVoto voto) {
        kafkaTemplate.send(TOPIC, voto.idSessao(), voto);
    }

}
