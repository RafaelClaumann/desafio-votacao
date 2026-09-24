package com.votacao.application.service;

import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VotoService")
class VotoServiceTest {

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private SessaoService sessaoService;

    @Mock
    private DocumentoValidator documentoValidator;

    @InjectMocks
    private VotoService votoService;

    private static final Long ID_SESSAO = 1L;

    @Test
    @DisplayName("votar should normalize the documento before checking validation, duplicity and saving")
    void votar_shouldNormalizeDocumento_beforeCheckingValidationAndDuplicityAndSaving() {
        String documento = "123.456.789-09";

        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(true);
        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(votoRepository.existsBySessaoIdAndDocumento(ID_SESSAO, "12345678909")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voto result = votoService.votar(ID_SESSAO, documento, Voto.Escolha.SIM);

        assertEquals("12345678909", result.documento());
        verify(votoRepository).existsBySessaoIdAndDocumento(ID_SESSAO, "12345678909");

        ArgumentCaptor<Voto> captor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).save(captor.capture());
        assertEquals("12345678909", captor.getValue().documento());
    }

    @Test
    @DisplayName("votar should throw DuplicatedVoteException when the documento already voted in the session ignoring formatting")
    void votar_shouldReject_whenDocumentoAlreadyVotedInSessionIgnoringFormatting() {
        String documento = "123.456.789-09";

        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(true);
        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(votoRepository.existsBySessaoIdAndDocumento(ID_SESSAO, "12345678909")).thenReturn(true);

        DuplicatedVoteException exception = assertThrows(
                DuplicatedVoteException.class,
                () -> votoService.votar(ID_SESSAO, documento, Voto.Escolha.SIM)
        );

        assertEquals(
                "Duplicated vote for document: 12345678909 in session with id: " + ID_SESSAO,
                exception.getMessage()
        );
        verify(votoRepository, never()).save(any(Voto.class));
    }

    @Test
    @DisplayName("votar should save the vote when documento is valid, the session is open and the documento is new")
    void votar_shouldSaveVote_whenValidDocumentoSessionIsOpenAndDocumentoIsNew() {
        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(true);
        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(votoRepository.existsBySessaoIdAndDocumento(ID_SESSAO, "12345678909")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Voto result = votoService.votar(ID_SESSAO, "12345678909", Voto.Escolha.NAO);

        assertEquals("12345678909", result.documento());
        assertEquals(Voto.Escolha.NAO, result.escolhaVoto());
        assertEquals(ID_SESSAO, result.sessao().id());
        verify(votoRepository).existsBySessaoIdAndDocumento(ID_SESSAO, "12345678909");
        verify(votoRepository).save(any(Voto.class));
    }

    private Sessao openSessao() {
        return new Sessao(
                ID_SESSAO,
                new Pauta(1L, "Reforma estatutária do capítulo quatro", 10L),
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(9)
        );
    }

}
