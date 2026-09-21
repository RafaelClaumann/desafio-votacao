package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;

public record PautaDTO(String titulo, Long tempoVotacaoMinutos) {

    public static PautaDTO fromDomain(Pauta domain) {
        return new PautaDTO(domain.titulo(), domain.tempoVotacaoMinutos());
    }

    public static Pauta toDomain(PautaDTO dto) {
        return new Pauta(dto.titulo(), dto.tempoVotacaoMinutos());
    }

}
