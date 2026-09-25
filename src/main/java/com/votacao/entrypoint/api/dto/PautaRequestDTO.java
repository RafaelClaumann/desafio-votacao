package com.votacao.entrypoint.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

        @Positive(message = "O tempo de votação deve ser maior que zero")
        @Max(value = 43200, message = "O tempo de votação não pode exceder 43200 minutos (30 dias)")
        @NotNull(message = "O tempo de votação é obrigatório")
        Long tempoVotacaoMinutos
) {
}
