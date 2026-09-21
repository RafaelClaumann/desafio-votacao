package com.votacao.entrypoint.persistence;

import com.votacao.application.model.Pauta;
import com.votacao.entrypoint.persistence.entity.PautaEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataPautaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PautaRepositoryAdapterTest {

    @Mock
    private SpringDataPautaRepository jpaRepository;

    @InjectMocks
    private PautaRepositoryAdapter adapter;

    @Test
    void save_shouldSendPautaDataToRepositoryWithoutId() {
        Pauta pauta = new Pauta(null, "Discuss the new voting rules", 10L);
        PautaEntity savedEntity = new PautaEntity("Discuss the new voting rules", 10L);

        when(jpaRepository.save(any(PautaEntity.class))).thenReturn(savedEntity);

        adapter.save(pauta);

        ArgumentCaptor<PautaEntity> entityCaptor = ArgumentCaptor.forClass(PautaEntity.class);
        verify(jpaRepository).save(entityCaptor.capture());
        PautaEntity capturedEntity = entityCaptor.getValue();

        assertAll(
                () -> assertNull(capturedEntity.getId()),
                () -> assertEquals(pauta.titulo(), capturedEntity.getTitulo()),
                () -> assertEquals(pauta.tempoVotacaoMinutos(), capturedEntity.getTempoVotacaoMinutos())
        );
    }

    @Test
    void save_shouldReturnPautaWithGeneratedId() {
        Pauta pauta = new Pauta(null, "Discuss the new voting rules", 10L);
        PautaEntity savedEntity = new PautaEntity(pauta.titulo(), pauta.tempoVotacaoMinutos());
        ReflectionTestUtils.setField(savedEntity, "id", 1L);

        when(jpaRepository.save(any(PautaEntity.class))).thenReturn(savedEntity);

        Pauta result = adapter.save(pauta);

        assertAll(
                () -> assertEquals(1L, result.id()),
                () -> assertEquals(pauta.titulo(), result.titulo()),
                () -> assertEquals(pauta.tempoVotacaoMinutos(), result.tempoVotacaoMinutos())
        );
    }

    @Test
    void getPautas_shouldReturnEmptyListWhenNoEntities() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<Pauta> pautas = adapter.getPautas();

        assertNotNull(pautas);
        assertTrue(pautas.isEmpty());
    }

}
