package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Pauta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PautaRequestDTO(
        @Size(min = 20, max = 150)
        @NotBlank(message = "O título é obrigatório")
        String titulo,

        @NotNull(message = "O tempo de votação é obrigatório")
        Long tempoVotacaoMinutos
) {

    public static PautaRequestDTO fromDomain(Pauta domain) {
        return new PautaRequestDTO(domain.titulo(), domain.tempoVotacaoMinutos());
    }

    public static Pauta toDomain(PautaRequestDTO dto) {
        return new Pauta(null, dto.titulo(), dto.tempoVotacaoMinutos());
    }

    public static List<PautaRequestDTO> toDTOList(List<Pauta> pautas) {
        return pautas.stream().map(PautaRequestDTO::fromDomain).toList();
    }

}
