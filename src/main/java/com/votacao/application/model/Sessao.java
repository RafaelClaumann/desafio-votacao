package com.votacao.application.model;

import java.time.LocalDateTime;

/**
 * Representa uma sessão de votação vinculada a uma pauta.
 *
 * @param id identificador da sessão
 * @param pauta pauta associada à sessão
 * @param startedAt instante em que a sessão foi aberta
 * @param expiresAt instante em que a sessão expira
 */
public record Sessao(
        Long id,
        Pauta pauta,
        LocalDateTime startedAt,
        LocalDateTime expiresAt
) {

    /**
     * Verifica se a sessão está aberta em um determinado instante.
     *
     * @param now instante de referência
     * @return {@code true} quando a sessão ainda está aberta; {@code false} caso contrário
     */
    public boolean isOpen(LocalDateTime now) {
        return expiresAt != null && now.isBefore(expiresAt);
    }

}
