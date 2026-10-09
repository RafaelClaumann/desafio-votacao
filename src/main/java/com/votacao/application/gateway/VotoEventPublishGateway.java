package com.votacao.application.gateway;

import com.votacao.application.service.query.VotoPublishData;

public interface VotoEventPublishGateway {

    void publish(VotoPublishData votoData);

}
