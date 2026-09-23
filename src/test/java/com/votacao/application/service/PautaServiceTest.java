package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.application.model.exception.PautaNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PautaService")
class PautaServiceTest {

    @Mock
    private PautaRepository repository;

    @InjectMocks
    private PautaService service;

    @Test
    @DisplayName("savePauta should save the pauta when the title is unique")
    void savePauta_shouldCreatePauta_whenTitleIsUnique() {
        Pauta pauta = new Pauta(null, "Reforma estatutária do capítulo quatro", 10L);
        Pauta saved = new Pauta(1L, pauta.titulo(), pauta.tempoVotacaoMinutos());

        when(repository.existsByTituloIgnoreCase(pauta.titulo())).thenReturn(false);
        when(repository.save(pauta)).thenReturn(saved);

        Pauta result = service.savePauta(pauta);

        assertEquals(saved, result);
        verify(repository).existsByTituloIgnoreCase(pauta.titulo());
        verify(repository).save(pauta);
    }

    @Test
    @DisplayName("savePauta should throw DuplicatedPautaException when the title already exists ignoring case")
    void savePauta_shouldThrow_whenTitleAlreadyExistsIgnoringCase() {
        Pauta pauta = new Pauta(null, "reforma estatutária do capítulo quatro", 10L);

        when(repository.existsByTituloIgnoreCase(pauta.titulo())).thenReturn(true);

        DuplicatedPautaException exception = assertThrows(
                DuplicatedPautaException.class,
                () -> service.savePauta(pauta)
        );

        assertEquals("Já existe uma pauta com o título: " + pauta.titulo(), exception.getMessage());
        verify(repository, never()).save(any(Pauta.class));
    }

    @Test
    @DisplayName("savePauta should throw DuplicatedPautaException when the repository rejects the title")
    void savePauta_shouldThrow_whenRepositoryRejectsTitle() {
        Pauta pauta = new Pauta(null, "Reforma estatutária do capítulo quatro", 10L);

        when(repository.existsByTituloIgnoreCase(pauta.titulo())).thenReturn(false);
        when(repository.save(pauta)).thenThrow(new DuplicatedPautaException(pauta.titulo()));

        DuplicatedPautaException exception = assertThrows(
                DuplicatedPautaException.class,
                () -> service.savePauta(pauta)
        );

        assertEquals("Já existe uma pauta com o título: " + pauta.titulo(), exception.getMessage());
    }

    @Test
    @DisplayName("getPautaById should throw PautaNotFoundException when the pauta does not exist")
    void getPautaById_shouldThrow_whenPautaDoesNotExist() {
        Long pautaId = 1L;

        when(repository.findById(pautaId)).thenReturn(java.util.Optional.empty());

        PautaNotFoundException exception = assertThrows(
                PautaNotFoundException.class,
                () -> service.getPautaById(pautaId)
        );

        assertEquals("Pauta not found with id: " + pautaId, exception.getMessage());
        verify(repository).findById(pautaId);
    }

}