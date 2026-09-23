package com.votacao.entrypoint.api.dto;

/**
 * Payload para abertura de uma sessão de votação.
 *
 * @param pautaId identificador da pauta vinculada à sessão
 */
public record SessaoDTO(
        Long pautaId
) {
}
