package com.votacao.application.service;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.model.exception.PautaNotFoundException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import com.votacao.application.service.query.SessaoComStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    private static final LocalDateTime BASE_DATE_TIME = LocalDateTime.of(2026, 9, 23, 10, 0);
    private static final Pauta PAUTA = new Pauta(2L, "Reforma estatutária do capítulo quatro", 10L);

    @Test
    @DisplayName("saveSessao should create and save a session with the pauta duration")
    void saveSessao_shouldCreateAndSaveSessionWithPautaDuration() {
        Long idPauta = 1L;
        Pauta pauta = new Pauta(idPauta, "Reforma estatutária do capítulo quatro", 10L);
        LocalDateTime now = LocalDateTime.of(2026, 9, 23, 10, 0);

        when(pautaService.getPautaById(idPauta)).thenReturn(pauta);
        when(sessaoRepository.existsByIdPauta(idPauta)).thenReturn(false);
        when(sessaoRepository.now()).thenReturn(now);
        when(sessaoRepository.save(any(Sessao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Sessao result = sessaoService.saveSessao(idPauta);

        ArgumentCaptor<Sessao> sessaoCaptor = ArgumentCaptor.forClass(Sessao.class);
        verify(sessaoRepository).save(sessaoCaptor.capture());
        Sessao savedSession = sessaoCaptor.getValue();

        assertAll(
                () -> assertEquals(pauta, savedSession.pauta()),
                () -> assertEquals(pauta, result.pauta()),
                () -> assertEquals(now, savedSession.startedAt()),
                () -> assertEquals(now.plusMinutes(pauta.tempoVotacaoMinutos()), savedSession.expiresAt()),
                () -> assertEquals(savedSession.startedAt(), result.startedAt()),
                () -> assertEquals(savedSession.expiresAt(), result.expiresAt())
        );
        verify(pautaService).getPautaById(idPauta);
    }

    @Test
    @DisplayName("saveSessao should throw PautaNotFoundException when the pauta does not exist")
    void saveSessao_shouldThrowWhenPautaDoesNotExist() {
        Long idPauta = 1L;

        when(pautaService.getPautaById(idPauta)).thenThrow(new PautaNotFoundException(idPauta));

        PautaNotFoundException exception = assertThrows(
                PautaNotFoundException.class,
                () -> sessaoService.saveSessao(idPauta)
        );

        assertEquals("Pauta com id " + idPauta + " não encontrada", exception.getMessage());
        verify(sessaoRepository, never()).existsByIdPauta(anyLong());
        verify(sessaoRepository, never()).save(any(Sessao.class));
    }

    @Test
    @DisplayName("saveSessao should throw DuplicatedSessaoException when the pauta already has a session")
    void saveSessao_shouldThrowWhenSessionAlreadyExistsForPauta() {
        Long idPauta = 1L;
        Pauta pauta = new Pauta(idPauta, "Reforma estatutária do capítulo quatro", 10L);

        when(pautaService.getPautaById(idPauta)).thenReturn(pauta);
        when(sessaoRepository.existsByIdPauta(idPauta)).thenReturn(true);

        DuplicatedSessaoException exception = assertThrows(
                DuplicatedSessaoException.class,
                () -> sessaoService.saveSessao(idPauta)
        );

        assertEquals("Já existe uma sessão para a pauta: " + idPauta, exception.getMessage());
        verify(sessaoRepository, never()).save(any(Sessao.class));
    }

    @Test
    @DisplayName("getOpenSessaoById should return the session when it is open")
    void getOpenSessaoById_shouldReturnOpenSession() {
        Long idSessao = 1L;
        Sessao sessao = new Sessao(idSessao, PAUTA, BASE_DATE_TIME, BASE_DATE_TIME.plusMinutes(10));

        when(sessaoRepository.findById(idSessao)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.now()).thenReturn(BASE_DATE_TIME);

        Sessao result = sessaoService.getOpenSessaoById(idSessao);

        assertEquals(sessao, result);
        verify(sessaoRepository).findById(idSessao);
        verify(sessaoRepository).now();
    }

    @Test
    @DisplayName("getOpenSessaoById should throw SessaoNotFoundException when the session does not exist")
    void getOpenSessaoById_shouldThrowWhenSessionDoesNotExist() {
        Long idSessao = 1L;

        when(sessaoRepository.findById(idSessao)).thenReturn(Optional.empty());

        assertThrows(SessaoNotFoundException.class, () -> sessaoService.getOpenSessaoById(idSessao));

        verify(sessaoRepository).findById(idSessao);
    }

    @Test
    @DisplayName("getOpenSessaoById should throw SessaoIsClosedException when the session is closed")
    void getOpenSessaoById_shouldThrowWhenSessionIsClosed() {
        Long idSessao = 1L;
        Sessao closed = new Sessao(idSessao, PAUTA, BASE_DATE_TIME.minusMinutes(20), BASE_DATE_TIME.minusMinutes(10));

        when(sessaoRepository.findById(idSessao)).thenReturn(Optional.of(closed));
        when(sessaoRepository.now()).thenReturn(BASE_DATE_TIME);

        SessaoIsClosedException exception = assertThrows(
                SessaoIsClosedException.class,
                () -> sessaoService.getOpenSessaoById(idSessao)
        );

        assertEquals("A sessão " + idSessao + " está fechada", exception.getMessage());
        verify(sessaoRepository).findById(idSessao);
        verify(sessaoRepository).now();
    }

    @Test
    @DisplayName("getClosedSessaoById should return the session when it is closed")
    void getClosedSessaoById_shouldReturnClosedSession() {
        Long idSessao = 1L;
        Sessao closed = new Sessao(idSessao, PAUTA, BASE_DATE_TIME.minusMinutes(20), BASE_DATE_TIME.minusMinutes(10));

        when(sessaoRepository.findById(idSessao)).thenReturn(Optional.of(closed));
        when(sessaoRepository.now()).thenReturn(BASE_DATE_TIME);

        Sessao result = sessaoService.getClosedSessaoById(idSessao);

        assertEquals(closed, result);
        verify(sessaoRepository).findById(idSessao);
        verify(sessaoRepository).now();
    }

    @Test
    @DisplayName("getClosedSessaoById should throw SessaoIsOpenException when the session is still open")
    void getClosedSessaoById_shouldThrowWhenSessionIsOpen() {
        Long idSessao = 1L;
        Sessao open = new Sessao(idSessao, PAUTA, BASE_DATE_TIME, BASE_DATE_TIME.plusMinutes(10));

        when(sessaoRepository.findById(idSessao)).thenReturn(Optional.of(open));
        when(sessaoRepository.now()).thenReturn(BASE_DATE_TIME);

        SessaoIsOpenException exception = assertThrows(
                SessaoIsOpenException.class,
                () -> sessaoService.getClosedSessaoById(idSessao)
        );

        assertEquals("A sessão " + idSessao + " ainda está aberta", exception.getMessage());
        verify(sessaoRepository).findById(idSessao);
        verify(sessaoRepository).now();
    }

    @Test
    @DisplayName("getOpenSessaoById should propagate the repository exception")
    void getOpenSessaoById_shouldPropagateRepositoryException() {
        Long idSessao = 1L;
        IllegalStateException repositoryException = new IllegalStateException("Repository unavailable");

        when(sessaoRepository.findById(idSessao)).thenThrow(repositoryException);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> sessaoService.getOpenSessaoById(idSessao)
        );

        assertEquals(repositoryException, exception);
        verify(sessaoRepository).findById(idSessao);
    }

    @Test
    @DisplayName("getSessoesComStatus should pair each session with its open status")
    void getSessoesComStatus_shouldPairSessionsWithOpenStatus() {
        Sessao open = new Sessao(1L, PAUTA, BASE_DATE_TIME, BASE_DATE_TIME.plusMinutes(10));
        Sessao closed = new Sessao(2L, PAUTA, BASE_DATE_TIME.minusMinutes(20), BASE_DATE_TIME.minusMinutes(10));

        when(sessaoRepository.findAll()).thenReturn(List.of(open, closed));
        when(sessaoRepository.now()).thenReturn(BASE_DATE_TIME);

        List<SessaoComStatus> result = sessaoService.getSessoesComStatus();

        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals(open, result.get(0).sessao()),
                () -> assertTrue(result.get(0).isOpen()),
                () -> assertEquals(closed, result.get(1).sessao()),
                () -> assertFalse(result.get(1).isOpen())
        );
        verify(sessaoRepository).findAll();
        verify(sessaoRepository).now();
    }

}