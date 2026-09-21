package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Sessao;

public record SessaoDTO(
        Long pautaId
) {

    public static SessaoDTO fromDomain(Sessao saved) {
        return new SessaoDTO(saved.id());

    }

}
