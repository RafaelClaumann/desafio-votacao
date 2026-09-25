package com.votacao.entrypoint.api.dto;

import jakarta.validation.constraints.NotNull;

public record SessaoDTO(
        @NotNull(message = "O id da Pauta é obrigatório")
        Long idPauta
) {
}
