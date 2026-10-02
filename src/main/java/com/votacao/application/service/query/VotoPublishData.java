package com.votacao.application.service.query;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;

public record VotoPublishData(
        Pauta pauta,
        Sessao sessao,
        Voto voto
) {
}
