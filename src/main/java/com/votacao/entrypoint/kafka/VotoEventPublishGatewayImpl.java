package com.votacao.entrypoint.kafka;

import com.votacao.application.gateway.VotoEventPublishGateway;
import com.votacao.application.service.query.VotoPublishData;
import com.votacao.entrypoint.mapper.VotoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
public class VotoEventPublishGatewayImpl implements VotoEventPublishGateway {

    private static final Logger log = LoggerFactory.getLogger(VotoEventPublishGatewayImpl.class);
    private static final String TOPIC = "voto-topic";

    private final KafkaTemplate<String, VotoEvent> kafkaTemplate;
    private final VotoMapper votoMapper;

    public VotoEventPublishGatewayImpl(KafkaTemplate<String, VotoEvent> kafkaTemplate, VotoMapper votoMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.votoMapper = votoMapper;
    }

    @Override
    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void publish(VotoPublishData votoData) {
        VotoEvent event = votoMapper.toEvent(votoData);
        CompletableFuture<SendResult<String, VotoEvent>> send = kafkaTemplate.send(TOPIC, String.valueOf(event.idSessao()), event);

        send.whenComplete((record, throwable) -> {
            if (throwable != null) {
                log.error("Failed to send message: {}", throwable.getMessage());
            } else {
                log.info(
                        "Message sent successfully: idVoto={}, timestamp={}, partition={}",
                        record.getProducerRecord().value().idVoto(),
                        record.getRecordMetadata().timestamp(),
                        record.getRecordMetadata().partition()
                );
            }
        });
    }

}
