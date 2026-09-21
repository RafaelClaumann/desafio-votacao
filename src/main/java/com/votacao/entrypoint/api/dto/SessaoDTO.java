package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Sessao;

public record SessaoDTO(
        Long pautaId
) {

    public static SessaoDTO fromDomain(Sessao saved) {
        return new SessaoDTO(saved.id());

    }

    public static Sessao toDomain(SessaoDTO requestBody) {
        return new Sessao(null, null, null, null);
    }

}
