package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Voto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Payload para registro de um voto.
 *
 * @param idSessao identificador da sessão em que o voto será registrado
 * @param documento documento do eleitor, em formato de CPF
 * @param escolhaVoto escolha do eleitor
 */
public record VotoDTO(
        @NotNull(message = "O id da Sessão é obrigatório")
        Long idSessao,

        @CPF
        @NotBlank(message = "O documento é obrigatório")
        String documento,

        @NotNull(message = "A escolha do voto é obrigatória")
        Voto.Escolha escolhaVoto
) {

    /**
     * Converte uma entidade de domínio em um DTO de resposta.
     *
     * @param saved voto salvo
     * @return DTO representando o voto salvo
     */
    public static VotoDTO fromDomain(Voto saved) {
        return new VotoDTO(
                saved.sessao().id(),
                saved.documento(),
                saved.escolhaVoto()
        );
    }

}
