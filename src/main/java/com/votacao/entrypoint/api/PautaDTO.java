package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PautaDTO(
        @NotEmpty
        @Size(min = 20, max = 150)
        String titulo,

        @NotNull
        Long tempoVotacaoMinutos) {

    public static PautaDTO fromDomain(Pauta domain) {
        return new PautaDTO(domain.titulo(), domain.tempoVotacaoMinutos());
    }

    public static Pauta toDomain(PautaDTO dto) {
        return new Pauta(dto.titulo(), dto.tempoVotacaoMinutos());
    }

}
