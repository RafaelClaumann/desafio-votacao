package com.votacao.entrypoint.api.dto;

/**
 * Resposta HTTP com os dados de uma pauta.
 *
 * @param id identificador da pauta
 * @param titulo título da pauta
 * @param tempoVotacaoMinutos duração da sessão em minutos
 */
public record PautaResponseDTO(
        Long id,
        String titulo,
        Long tempoVotacaoMinutos
) {
}
