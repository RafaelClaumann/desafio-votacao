package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Voto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

public record VotoDTO(
        @NotNull(message = "O id da Sessão é obrigatório")
        Long idSessao,

        @CPF
        @NotBlank(message = "O documento é obrigatório")
        String documento,

        @NotNull(message = "A escolha do voto é obrigatória")
        Voto.Escolha escolhaVoto
) {

    public static VotoDTO fromDomain(Voto saved) {
        return new VotoDTO(
                saved.sessao().id(),
                saved.documento(),
                saved.escolhaVoto()
        );
    }

}
