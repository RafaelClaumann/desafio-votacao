package com.votacao.entrypoint.kafka;

import com.votacao.application.gateway.VotoEventPublishGateway;
import com.votacao.application.service.query.VotoPublishData;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class EventPublisherAdapter implements VotoEventPublishGateway {

    private final ApplicationEventPublisher publisher;

    public EventPublisherAdapter(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(VotoPublishData votoData) {
        publisher.publishEvent(votoData);
    }

}
