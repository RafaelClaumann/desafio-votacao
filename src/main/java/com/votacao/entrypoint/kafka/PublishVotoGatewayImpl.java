package com.votacao.entrypoint.kafka;

import com.votacao.application.gateway.PublishVotoGateway;
import com.votacao.application.service.query.VotoPublishData;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublishVotoGatewayImpl implements PublishVotoGateway {

    private static final String TOPIC = "voto-topic";

    private final KafkaTemplate<String, VotoPublishData> kafkaTemplate;

    public PublishVotoGatewayImpl(KafkaTemplate<String, VotoPublishData> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishVoto(VotoPublishData voto) {
        Long id = voto.sessao().id();
        kafkaTemplate.send(TOPIC, String.valueOf(id), voto);
    }

}
