package com.votacao.entrypoint.kafka;

import com.votacao.application.gateway.PublishVotoGateway;
import com.votacao.application.service.query.VotoPublishData;
import com.votacao.entrypoint.mapper.VotoMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PublishVotoGatewayImpl implements PublishVotoGateway {

    private static final String TOPIC = "voto-topic";

    private final KafkaTemplate<String, VotoEvent> kafkaTemplate;
    private final VotoMapper votoMapper;

    public PublishVotoGatewayImpl(
            KafkaTemplate<String, VotoEvent> kafkaTemplate,
            VotoMapper votoMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.votoMapper = votoMapper;
    }

    @Override
    public void publishVoto(VotoPublishData voto) {
        VotoEvent event = votoMapper.toEvent(voto);
        kafkaTemplate.send(TOPIC, String.valueOf(event.idSessao()), event);
    }

}
