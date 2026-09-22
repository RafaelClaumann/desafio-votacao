package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
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

}
