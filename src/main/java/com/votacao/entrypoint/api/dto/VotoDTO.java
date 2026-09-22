package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Voto;

public record VotoDTO(Long idSessao, String documento, String escolhaVoto) {

    public static VotoDTO fromDomain(Voto saved) {
        return new VotoDTO(
                saved.sessao().id(),
                saved.documento(),
                saved.escolhaVoto().name()
        );
    }

}
