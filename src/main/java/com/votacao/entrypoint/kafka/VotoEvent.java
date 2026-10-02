package com.votacao.entrypoint.kafka;

public record VotoEvent(
        long idPauta,
        String titulo,
        long tempoVotacaoMinutos,
        long idSessao,
        String startedAt,
        String expiresAt,
        long idVoto,
        String documento,
        String escolhaVoto,
        String publishedAt
) {
}
