package com.votacao.entrypoint.persistence;

import com.votacao.application.model.Pauta;
import com.votacao.entrypoint.persistence.jpa.SpringDataPautaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PautaRepositoryAdapterTest {

    @Mock
    private SpringDataPautaRepository jpaRepository;

    @InjectMocks
    private PautaRepositoryAdapter adapter;

    @Test
    void getPautas_shouldReturnEmptyListWhenNoEntities() {
        when(jpaRepository.findAll()).thenReturn(List.of());

        List<Pauta> pautas = adapter.getPautas();

        assertNotNull(pautas);
        assertTrue(pautas.isEmpty());
    }

}
