package com.votacao.application.service.query;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PublishVoto(
        long idSessao,
        long idPauta,
        long idVoto,
        String documento,
        String tituloPauta,
        String escolhaVoto,
        LocalDateTime publishedAt
) {

}
