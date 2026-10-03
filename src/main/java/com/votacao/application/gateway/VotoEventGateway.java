package com.votacao.application.gateway;

import com.votacao.application.service.query.VotoPublishData;

public interface VotoEventGateway {

    void publish(VotoPublishData voto);

}
