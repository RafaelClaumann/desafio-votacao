package com.votacao.entrypoint.api.dto;

import java.time.LocalDateTime;

/**
 * Resposta HTTP com os dados de uma sessão.
 *
 * @param id identificador da sessão
 * @param idPauta identificador da pauta vinculada
 * @param startedAt instante de abertura da sessão
 * @param expiresAt instante de expiração da sessão
 * @param isOpen indica se a sessão está aberta no momento da resposta
 */
public record SessaoResponseDTO(
        Long id,
        Long idPauta,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        boolean isOpen
) {
}
