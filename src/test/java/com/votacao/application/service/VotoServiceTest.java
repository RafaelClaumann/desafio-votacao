package com.votacao.application.service;

import br.com.caelum.stella.format.Formatter;
import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.application.gateway.TimeProvider;
import com.votacao.application.gateway.VotoEventPublishGateway;
import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.service.query.VotoPublishData;
import org.junit.jupiter.api.BeforeEach;
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
    private TimeProvider timeProvider;

    @Mock
    private VotoEventPublishGateway votoEventPublishGateway;

    @Mock
    private DocumentoValidator documentoValidator;

    @Mock
    private Formatter formatter;

    @InjectMocks
    private VotoService votoService;

    private static final Long ID_SESSAO = 1L;
    private static final String DOCUMENTO_NAO_NORMALIZADO = "123.456.789-09";
    private static final String DOCUMENTO_NORMALIZADO = "12345678909";
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 10, 3, 10, 0, 0);

    private static final Pauta PAUTA = new Pauta(1L, "Reforma estatutária do capítulo quatro", 10L);
    private static final Sessao SESSAO = new Sessao(ID_SESSAO, PAUTA, FIXED_NOW, FIXED_NOW.plusMinutes(9));

    @BeforeEach
    void setUp() {
        when(sessaoService.getOpenSessaoById(ID_SESSAO)).thenReturn(SESSAO);
        when(formatter.unformat(DOCUMENTO_NAO_NORMALIZADO)).thenReturn(DOCUMENTO_NORMALIZADO);
        when(documentoValidator.isValidDocumento(DOCUMENTO_NORMALIZADO)).thenReturn(true);
    }


    @Test
    @DisplayName("votar should save the vote when documento is valid, the session is open and the documento is new")
    void votar_shouldSaveVote_whenValidDocumentoSessionIsOpenAndDocumentoIsNew() {

        when(votoRepository.existsByIdSessaoAndDocumento(ID_SESSAO, DOCUMENTO_NORMALIZADO)).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> {
            Voto argument = invocation.getArgument(0);
            return new Voto(
                    1L,
                    argument.sessao(),
                    argument.documento(),
                    argument.escolhaVoto()
            );
        });
        when(timeProvider.now()).thenReturn(FIXED_NOW);

        Voto result = votoService.votar(ID_SESSAO, DOCUMENTO_NAO_NORMALIZADO, Voto.Escolha.NAO);

        assertEquals(DOCUMENTO_NORMALIZADO, result.documento());
        assertEquals(Voto.Escolha.NAO, result.escolhaVoto());
        assertEquals(ID_SESSAO, result.sessao().id());
        verify(votoRepository).existsByIdSessaoAndDocumento(ID_SESSAO, DOCUMENTO_NORMALIZADO);
        verify(votoRepository).save(any(Voto.class));
        verify(votoEventPublishGateway, times(1)).publish(any(VotoPublishData.class));
    }

    @Test
    @DisplayName("votar should normalize the documento before duplicity validation and saving")
    void votar_shouldNormalizeDocumento_beforeDuplicityValidationAndSaving() {

        when(votoRepository.existsByIdSessaoAndDocumento(ID_SESSAO, DOCUMENTO_NORMALIZADO)).thenReturn(false);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> {
            Voto argument = invocation.getArgument(0);
            return new Voto(
                    1L,
                    argument.sessao(),
                    argument.documento(),
                    argument.escolhaVoto()
            );
        });
        when(timeProvider.now()).thenReturn(FIXED_NOW);

        Voto result = votoService.votar(ID_SESSAO, DOCUMENTO_NAO_NORMALIZADO, Voto.Escolha.SIM);

        assertEquals(DOCUMENTO_NORMALIZADO, result.documento());
        verify(votoRepository).existsByIdSessaoAndDocumento(ID_SESSAO, DOCUMENTO_NORMALIZADO);

        ArgumentCaptor<Voto> captor = ArgumentCaptor.forClass(Voto.class);
        verify(votoRepository).save(captor.capture());
        assertEquals(DOCUMENTO_NORMALIZADO, captor.getValue().documento());

        verify(votoEventPublishGateway, times(1)).publish(any(VotoPublishData.class));
        verify(timeProvider, times(1)).now();
    }

    @Test
    @DisplayName("votar should publish VotoPublishData with correct fields and publishedAt")
    void votar_shouldPublishVotoPublishData_withCorrectFields() {

        when(votoRepository.existsByIdSessaoAndDocumento(any(Long.class), any(String.class))).thenReturn(false);
        when(timeProvider.now()).thenReturn(FIXED_NOW);
        when(votoRepository.save(any(Voto.class))).thenAnswer(invocation -> {
            Voto argument = invocation.getArgument(0);
            return new Voto(
                    1L,
                    argument.sessao(),
                    argument.documento(),
                    argument.escolhaVoto()
            );
        });
        when(timeProvider.now()).thenReturn(FIXED_NOW);

        votoService.votar(ID_SESSAO, DOCUMENTO_NAO_NORMALIZADO, Voto.Escolha.SIM);

        ArgumentCaptor<VotoPublishData> captor = ArgumentCaptor.forClass(VotoPublishData.class);
        verify(votoEventPublishGateway).publish(captor.capture());

        VotoPublishData data = captor.getValue();
        assertEquals(SESSAO.pauta(), data.pauta());
        assertEquals(SESSAO.id(), data.sessao().id());
        assertEquals(1L, data.voto().id());
        assertEquals(DOCUMENTO_NORMALIZADO, data.voto().documento());
        assertEquals(Voto.Escolha.SIM, data.voto().escolhaVoto());
        assertEquals(FIXED_NOW, data.publishedAt());
    }

    @Test
    @DisplayName("votar should throw DuplicatedVoteException when the documento already voted in the session ignoring formatting")
    void votar_shouldReject_whenDocumentoAlreadyVotedInSessionIgnoringFormatting() {

        when(votoRepository.existsByIdSessaoAndDocumento(ID_SESSAO, DOCUMENTO_NORMALIZADO)).thenReturn(true);

        DuplicatedVoteException exception = assertThrows(
                DuplicatedVoteException.class,
                () -> votoService.votar(ID_SESSAO, DOCUMENTO_NAO_NORMALIZADO, Voto.Escolha.SIM)
        );

        assertEquals(
                "O documento 12345678909 já votou na sessão " + ID_SESSAO,
                exception.getMessage()
        );
        verify(votoRepository, never()).save(any(Voto.class));
        verify(votoEventPublishGateway, never()).publish(any(VotoPublishData.class));
        verify(timeProvider, never()).now();
    }

    @Test
    @DisplayName("votar should throw InvalidDocumentoException when the documento is invalid")
    void votar_shouldReject_whenDocumentoIsNotValid() {

        when(documentoValidator.isValidDocumento(DOCUMENTO_NORMALIZADO)).thenReturn(false);

        InvalidDocumentoException exception = assertThrows(
                InvalidDocumentoException.class,
                () -> votoService.votar(ID_SESSAO, DOCUMENTO_NAO_NORMALIZADO, Voto.Escolha.SIM)
        );

        assertEquals(
                "Documento inválido: " + DOCUMENTO_NAO_NORMALIZADO,
                exception.getMessage()
        );
        verify(sessaoService).getOpenSessaoById(ID_SESSAO);
        verify(votoRepository, never()).save(any(Voto.class));
        verify(votoEventPublishGateway, never()).publish(any(VotoPublishData.class));
        verify(timeProvider, never()).now();
    }

}
