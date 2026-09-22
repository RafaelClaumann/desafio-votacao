package com.votacao.application.service.query;

import com.votacao.application.model.Pauta;

public record PautaComStatus(Pauta pauta, boolean hasSessao) {
}
