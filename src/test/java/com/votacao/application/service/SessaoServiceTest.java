package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.SessaoIsClosedException;
import com.votacao.application.model.SessaoNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaRepository pautaRepository;

    @InjectMocks
    private SessaoService service;

    @Test
    void saveSessao_shouldCreateAndSaveSessionWithPautaDuration() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta(pautaId, "Discuss the new voting rules", 10L);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.existsByPautaId(pautaId)).thenReturn(false);
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sessao result = service.saveSessao(pautaId);

        ArgumentCaptor<Sessao> sessaoCaptor = ArgumentCaptor.forClass(Sessao.class);
        verify(sessaoRepository).save(sessaoCaptor.capture());
        Sessao savedSession = sessaoCaptor.getValue();

        assertAll(
                () -> assertEquals(pauta, savedSession.pauta()),
                () -> assertEquals(pauta, result.pauta()),
                () -> assertEquals(
                        Duration.ofMinutes(pauta.tempoVotacaoMinutos()),
                        Duration.between(savedSession.startedAt(), savedSession.expiresAt())
                ),
                () -> assertEquals(savedSession.startedAt(), result.startedAt()),
                () -> assertEquals(savedSession.expiresAt(), result.expiresAt())
        );
    }

    @Test
    void saveSessao_shouldThrowWhenPautaDoesNotExist() {
        Long pautaId = 1L;

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSessao(pautaId)
        );

        assertEquals("Pauta not found", exception.getMessage());
        verify(sessaoRepository, never()).existsByPautaId(any());
        verify(sessaoRepository, never()).save(any(Sessao.class));
    }

    @Test
    void saveSessao_shouldThrowWhenSessionAlreadyExistsForPauta() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta(pautaId, "Discuss the new voting rules", 10L);

        when(pautaRepository.findById(pautaId)).thenReturn(Optional.of(pauta));
        when(sessaoRepository.existsByPautaId(pautaId)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveSessao(pautaId)
        );

        assertEquals("Sessão already exists for this Pauta", exception.getMessage());
        verify(sessaoRepository, never()).save(any(Sessao.class));
    }

    @Test
    void getOpenSessaoById_shouldReturnOpenSession() {
        Long sessaoId = 1L;
        Sessao sessao = openSessao(sessaoId);

        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.of(sessao));

        Sessao result = service.getOpenSessaoById(sessaoId);

        assertEquals(sessao, result);
        verify(sessaoRepository).findById(sessaoId);
    }

    @Test
    void getOpenSessaoById_shouldThrowWhenSessionDoesNotExist() {
        Long sessaoId = 1L;

        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.empty());

        assertThrows(SessaoNotFoundException.class, () -> service.getOpenSessaoById(sessaoId));

        verify(sessaoRepository).findById(sessaoId);
    }

    @Test
    void getOpenSessaoById_shouldThrowWhenSessionIsClosed() {
        Long sessaoId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Sessao sessao = new Sessao(
                sessaoId,
                new Pauta(2L, "Discuss the new voting rules", 10L),
                now.minusMinutes(11),
                now.minusMinutes(1)
        );

        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.of(sessao));

        assertThrows(SessaoIsClosedException.class, () -> service.getOpenSessaoById(sessaoId));

        verify(sessaoRepository).findById(sessaoId);
    }

    @Test
    void getOpenSessaoById_shouldPropagateRepositoryException() {
        Long sessaoId = 1L;
        IllegalStateException repositoryException = new IllegalStateException("Repository unavailable");

        when(sessaoRepository.findById(sessaoId)).thenThrow(repositoryException);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.getOpenSessaoById(sessaoId)
        );

        assertEquals(repositoryException, exception);
        verify(sessaoRepository).findById(sessaoId);
    }

    private Sessao openSessao(Long sessaoId) {
        LocalDateTime now = LocalDateTime.now();
        return new Sessao(
                sessaoId,
                new Pauta(2L, "Discuss the new voting rules", 10L),
                now,
                now.plusMinutes(10)
        );
    }

}
