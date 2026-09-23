package com.votacao.entrypoint.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload para criação de uma pauta.
 *
 * @param titulo título da pauta
 * @param tempoVotacaoMinutos duração da sessão em minutos
 */
public record PautaRequestDTO(
        @Size(min = 20, max = 150)
        @NotBlank(message = "O título é obrigatório")
        String titulo,

        @NotNull(message = "O tempo de votação é obrigatório")
        Long tempoVotacaoMinutos
) {
}
