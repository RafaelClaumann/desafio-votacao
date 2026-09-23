package com.votacao.entrypoint.api.dto;

import com.votacao.application.model.Pauta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PautaRequestDTOTest {

    @Test
    void toDomain_shouldCreatePautaWithoutId() {
        PautaRequestDTO dto = new PautaRequestDTO("Discuss the new voting rules", 10L);

        Pauta result = PautaRequestDTO.toDomain(dto);

        assertAll(
                () -> assertNull(result.id()),
                () -> assertEquals(dto.titulo(), result.titulo()),
                () -> assertEquals(dto.tempoVotacaoMinutos(), result.tempoVotacaoMinutos())
        );
    }

}
