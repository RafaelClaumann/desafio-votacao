package com.votacao.application.service;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.model.exception.PautaNotFoundException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("SessaoService")
class SessaoServiceTest {

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaService pautaService;

    @InjectMocks
    private SessaoService sessaoService;

    @Test
    @DisplayName("saveSessao should create and save a session with the pauta duration")
    void saveSessao_shouldCreateAndSaveSessionWithPautaDuration() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta(pautaId, "Reforma estatutária do capítulo quatro", 10L);

        when(pautaService.getPautaById(pautaId)).thenReturn(pauta);
        when(sessaoRepository.existsByPautaId(pautaId)).thenReturn(false);
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sessao result = sessaoService.saveSessao(pautaId);

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
        verify(pautaService).getPautaById(pautaId);
    }

    @Test
    @DisplayName("saveSessao should throw PautaNotFoundException when the pauta does not exist")
    void saveSessao_shouldThrowWhenPautaDoesNotExist() {
        Long pautaId = 1L;

        when(pautaService.getPautaById(pautaId)).thenThrow(new PautaNotFoundException(pautaId));

        PautaNotFoundException exception = assertThrows(
                PautaNotFoundException.class,
                () -> sessaoService.saveSessao(pautaId)
        );

        assertEquals("Pauta not found with id: " + pautaId, exception.getMessage());
        verify(sessaoRepository, never()).existsByPautaId(any());
        verify(sessaoRepository, never()).save(any(Sessao.class));
    }

    @Test
    @DisplayName("saveSessao should throw DuplicatedSessaoException when the pauta already has a session")
    void saveSessao_shouldThrowWhenSessionAlreadyExistsForPauta() {
        Long pautaId = 1L;
        Pauta pauta = new Pauta(pautaId, "Reforma estatutária do capítulo quatro", 10L);

        when(pautaService.getPautaById(pautaId)).thenReturn(pauta);
        when(sessaoRepository.existsByPautaId(pautaId)).thenReturn(true);

        DuplicatedSessaoException exception = assertThrows(
                DuplicatedSessaoException.class,
                () -> sessaoService.saveSessao(pautaId)
        );

        assertEquals("Já existe uma sessão para a pauta: " + pautaId, exception.getMessage());
        verify(sessaoRepository, never()).save(any(Sessao.class));
    }

    @Test
    @DisplayName("getOpenSessaoById should return the session when it is open")
    void getOpenSessaoById_shouldReturnOpenSession() {
        Long sessaoId = 1L;
        Sessao sessao = openSessao(sessaoId);

        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.of(sessao));

        Sessao result = sessaoService.getOpenSessaoById(sessaoId);

        assertEquals(sessao, result);
        verify(sessaoRepository).findById(sessaoId);
    }

    @Test
    @DisplayName("getOpenSessaoById should throw SessaoNotFoundException when the session does not exist")
    void getOpenSessaoById_shouldThrowWhenSessionDoesNotExist() {
        Long sessaoId = 1L;

        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.empty());

        assertThrows(SessaoNotFoundException.class, () -> sessaoService.getOpenSessaoById(sessaoId));

        verify(sessaoRepository).findById(sessaoId);
    }

    @Test
    @DisplayName("getOpenSessaoById should throw SessaoIsClosedException when the session is closed")
    void getOpenSessaoById_shouldThrowWhenSessionIsClosed() {
        Long sessaoId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Sessao sessao = new Sessao(
                sessaoId,
                new Pauta(2L, "Reforma estatutária do capítulo quatro", 10L),
                now.minusMinutes(11),
                now.minusMinutes(1)
        );

        when(sessaoRepository.findById(sessaoId)).thenReturn(Optional.of(sessao));

        assertThrows(SessaoIsClosedException.class, () -> sessaoService.getOpenSessaoById(sessaoId));

        verify(sessaoRepository).findById(sessaoId);
    }

    @Test
    @DisplayName("getOpenSessaoById should propagate the repository exception")
    void getOpenSessaoById_shouldPropagateRepositoryException() {
        Long sessaoId = 1L;
        IllegalStateException repositoryException = new IllegalStateException("Repository unavailable");

        when(sessaoRepository.findById(sessaoId)).thenThrow(repositoryException);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> sessaoService.getOpenSessaoById(sessaoId)
        );

        assertEquals(repositoryException, exception);
        verify(sessaoRepository).findById(sessaoId);
    }

    private Sessao openSessao(Long sessaoId) {
        LocalDateTime now = LocalDateTime.now();
        return new Sessao(
                sessaoId,
                new Pauta(2L, "Reforma estatutária do capítulo quatro", 10L),
                now,
                now.plusMinutes(10)
        );
    }

}