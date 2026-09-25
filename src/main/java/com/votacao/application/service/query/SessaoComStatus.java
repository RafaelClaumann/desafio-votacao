package com.votacao.application.service.query;

import com.votacao.application.model.Sessao;

public record SessaoComStatus(
        Sessao sessao,
        boolean isOpen
) {

}