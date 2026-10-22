package com.votacao.entrypoint.kafka;

import com.votacao.application.service.query.VotoPublishData;
import com.votacao.entrypoint.mapper.VotoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.CompletableFuture;

import static com.votacao.entrypoint.filter.MDCRequestFilter.MDC_CORRELATION_ID_KEY;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

@Component
public class VotoEventKafkaSender {

    private static final Logger log = LoggerFactory.getLogger(VotoEventKafkaSender.class);
    private static final String TOPIC = "voto-topic";

    private final KafkaTemplate<String, VotoEvent> kafkaTemplate;
    private final VotoMapper votoMapper;

    public VotoEventKafkaSender(KafkaTemplate<String, VotoEvent> kafkaTemplate, VotoMapper votoMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.votoMapper = votoMapper;
    }

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void sendEvent(VotoPublishData votoData) {
        VotoEvent event = votoMapper.toEvent(votoData);
        String correlationId = MDC.get(MDC_CORRELATION_ID_KEY);

        try {
            CompletableFuture<SendResult<String, VotoEvent>> send = kafkaTemplate.send(TOPIC, String.valueOf(event.idSessao()), event);

            send.whenComplete((record, throwable) -> {
                if (throwable != null) {
                    log.error(
                            "Failed to send message: idVoto={}, idSessao={}, correlationId={}, reason={}",
                            event.idVoto(),
                            event.idSessao(),
                            correlationId,
                            throwable.getMessage()
                    );
                } else {
                    log.info(
                            "Message sent successfully: idVoto={}, timestamp={}, partition={}",
                            record.getProducerRecord().value().idVoto(),
                            record.getRecordMetadata().timestamp(),
                            record.getRecordMetadata().partition()
                    );
                }
            });
        } catch (RuntimeException ex) {
            log.error(
                    "Failed to send message synchronously: idVoto={}, idSessao={}, correlationId={}, reason={}",
                    event.idVoto(),
                    event.idSessao(),
                    correlationId,
                    ex.getMessage(),
                    ex
            );
        }
    }

}
