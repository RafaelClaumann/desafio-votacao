package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Sessao;

import java.util.List;

public record SessaoDTO(
        Long pautaId
) {

    public static SessaoDTO fromDomain(Sessao saved) {
        return new SessaoDTO(saved.id());
    }

    public static List<SessaoDTO> toDTOList(List<Sessao> sessoes) {
        return sessoes.stream().map(SessaoDTO::fromDomain).toList();
    }

}
