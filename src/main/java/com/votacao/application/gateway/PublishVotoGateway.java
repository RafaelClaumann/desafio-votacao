package com.votacao.application.gateway;

import com.votacao.application.service.query.VotoPublishData;

public interface PublishVotoGateway {

    void publishVoto(VotoPublishData voto);

}
