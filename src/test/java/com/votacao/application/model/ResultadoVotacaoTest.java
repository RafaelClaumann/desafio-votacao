package com.votacao.application.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ResultadoVotacao")
class ResultadoVotacaoTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, SEM_VOTOS",
            "1, 0, APROVADA",
            "0, 1, REJEITADA",
            "1, 1, EMPATE",
            "3, 2, APROVADA",
            "1, 2, REJEITADA",
            "2, 2, EMPATE"
    })
    @DisplayName("status should distinguish approval, rejection, real tie and empty result")
    void status_shouldReflectVoteCounts(long votosSim, long votosNao, StatusVotacao expected) {
        ResultadoVotacao resultado = new ResultadoVotacao(votosSim, votosNao);

        assertEquals(expected, resultado.status());
    }

    @Test
    @DisplayName("totalVotos should sum sim and nao counts")
    void totalVotos_shouldSumSimAndNao() {
        ResultadoVotacao resultado = new ResultadoVotacao(3, 2);

        assertEquals(5, resultado.totalVotos());
    }

}
