package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Sessao;

import java.time.LocalDateTime;
import java.util.List;

public record SessaoResponseDTO(
        Long idSessao,
        Long idPauta,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        boolean isOpen
) {

    public static SessaoResponseDTO fromDomain(Sessao sessao) {
        return new SessaoResponseDTO(
                sessao.id(),
                sessao.pauta().id(),
                sessao.startedAt(),
                sessao.expiresAt(),
                sessao.isOpen(LocalDateTime.now())
        );
    }

    public static List<SessaoResponseDTO> toDTOList(List<Sessao> sessoes) {
        return sessoes.stream().map(
                sessao -> new SessaoResponseDTO(
                        sessao.id(),
                        sessao.pauta().id(),
                        sessao.startedAt(),
                        sessao.expiresAt(),
                        sessao.isOpen(LocalDateTime.now())
                )
        ).toList();
    }
}
