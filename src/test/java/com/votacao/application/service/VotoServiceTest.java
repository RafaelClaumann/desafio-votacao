package com.votacao.application.service;

import br.com.caelum.stella.format.Formatter;
import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.application.gateway.PublishVotoGateway;
import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.service.query.VotoPublishData;
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
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("VotoService")
class VotoServiceTest {

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private SessaoService sessaoService;

    @Mock
    private PublishVotoGateway publishVotoGateway;

    @Mock
    private DocumentoValidator documentoValidator;

    @Mock
    private Formatter formatter;

    @InjectMocks
    private VotoService votoService;

    private static final Long ID_SESSAO = 1L;

    @Test
    @DisplayName("votar should normalize the documento before duplicity validation and saving")
    void votar_shouldNormalizeDocumento_beforeDuplicityValidationAndSaving() {
        String documento = "123.456.789-09";

        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(formatter.unformat("123.456.789-09")).thenReturn("12345678909");
        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(true);
        when(votoRepository.existsByIdSessaoAndDocumento(ID_SESSAO, "12345678909")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> {
            Voto argument = invocation.getArgument(0);
            return new Voto(
                    1L,
                    argument.sessao(),
                    argument.documento(),
                    argument.escolhaVoto(),
                    LocalDateTime.now()
            );
        });

        Voto result = votoService.votar(ID_SESSAO, documento, Voto.Escolha.SIM);

        assertEquals("12345678909", result.documento());
        verify(votoRepository).existsByIdSessaoAndDocumento(ID_SESSAO, "12345678909");

        ArgumentCaptor<Voto> captor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).save(captor.capture());
        assertEquals("12345678909", captor.getValue().documento());

        verify(publishVotoGateway, times(1)).publishEvent(any(VotoPublishData.class));
    }

    @Test
    @DisplayName("votar should save the vote when documento is valid, the session is open and the documento is new")
    void votar_shouldSaveVote_whenValidDocumentoSessionIsOpenAndDocumentoIsNew() {
        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(formatter.unformat("12345678909")).thenReturn("12345678909");
        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(true);
        when(votoRepository.existsByIdSessaoAndDocumento(ID_SESSAO, "12345678909")).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> {
            Voto argument = invocation.getArgument(0);
            return new Voto(
                    1L,
                    argument.sessao(),
                    argument.documento(),
                    argument.escolhaVoto(),
                    LocalDateTime.now()
            );
        });

        Voto result = votoService.votar(ID_SESSAO, "12345678909", Voto.Escolha.NAO);

        assertEquals("12345678909", result.documento());
        assertEquals(Voto.Escolha.NAO, result.escolhaVoto());
        assertEquals(ID_SESSAO, result.sessao().id());
        verify(votoRepository).existsByIdSessaoAndDocumento(ID_SESSAO, "12345678909");
        verify(votoRepository).save(any(Voto.class));
        verify(publishVotoGateway, times(1)).publishEvent(any(VotoPublishData.class));
    }

    @Test
    @DisplayName("votar should throw DuplicatedVoteException when the documento already voted in the session ignoring formatting")
    void votar_shouldReject_whenDocumentoAlreadyVotedInSessionIgnoringFormatting() {
        String documento = "123.456.789-09";

        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(formatter.unformat("123.456.789-09")).thenReturn("12345678909");
        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(true);
        when(votoRepository.existsByIdSessaoAndDocumento(ID_SESSAO, "12345678909")).thenReturn(true);

        DuplicatedVoteException exception = assertThrows(
                DuplicatedVoteException.class,
                () -> votoService.votar(ID_SESSAO, documento, Voto.Escolha.SIM)
        );

        assertEquals(
                "O documento 12345678909 já votou na sessão " + ID_SESSAO,
                exception.getMessage()
        );
        verify(votoRepository, never()).save(any(Voto.class));
        verify(publishVotoGateway, never()).publishEvent(any(VotoPublishData.class));

    }

    @Test
    @DisplayName("votar should throw InvalidDocumentoException when the documento is invalid")
    void votar_shouldReject_whenDocumentoIsNotValid() {
        String documento = "123.456.789-09";

        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(openSessao());
        when(formatter.unformat("123.456.789-09")).thenReturn("12345678909");
        when(documentoValidator.isValidDocumento("12345678909")).thenReturn(false);

        InvalidDocumentoException exception = assertThrows(
                InvalidDocumentoException.class,
                () -> votoService.votar(ID_SESSAO, documento, Voto.Escolha.SIM)
        );

        assertEquals(
                "Documento inválido: " + documento,
                exception.getMessage()
        );
        verify(sessaoService).getOpenSessaoById(ID_SESSAO);
        verify(votoRepository, never()).save(any(Voto.class));
        verify(publishVotoGateway, never()).publishEvent(any(VotoPublishData.class));
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
