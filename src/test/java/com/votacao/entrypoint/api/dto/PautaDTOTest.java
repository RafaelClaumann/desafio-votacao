package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Pauta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PautaDTOTest {

    @Test
    void toDomain_shouldCreatePautaWithoutId() {
        PautaDTO dto = new PautaDTO("Discuss the new voting rules", 10L);

        Pauta result = PautaDTO.toDomain(dto);

        assertAll(
                () -> assertNull(result.id()),
                () -> assertEquals(dto.titulo(), result.titulo()),
                () -> assertEquals(dto.tempoVotacaoMinutos(), result.tempoVotacaoMinutos())
        );
    }

}
