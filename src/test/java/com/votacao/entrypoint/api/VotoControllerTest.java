package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import com.votacao.application.service.VotoService;
import com.votacao.entrypoint.api.dto.VotoResponseDTO;
import com.votacao.entrypoint.mapper.VotoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VotoController.class)
@DisplayName("VotoController — POST /votos")
class VotoControllerTest {

    private static final String TITULO = "Reforma estatutária do capítulo quatro";
    private static final String DOCUMENTO_VALIDO = "52998224725";
    private static final String DOCUMENTO_INVALIDO = "11111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VotoService service;

    @MockitoBean
    private VotoMapper mapper;

    @Test
    @DisplayName("Should create the voto with the full response body when the input is valid")
    void save_shouldCreateVoto_whenInputIsValid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Pauta pauta = new Pauta(1L, TITULO, 10L);
        Sessao sessao = new Sessao(1L, pauta, now, now.plusMinutes(10));
        Voto saved = new Voto(1L, sessao, DOCUMENTO_VALIDO, Voto.Escolha.SIM);

        when(service.votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM)).thenReturn(saved);
        when(mapper.toResponse(any(Voto.class)))
                .thenReturn(new VotoResponseDTO(1L, 1L, 1L, TITULO, DOCUMENTO_VALIDO, Voto.Escolha.SIM, now, now.plusMinutes(10)));

        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.id_sessao").value(1))
                .andExpect(jsonPath("$.id_pauta").value(1))
                .andExpect(jsonPath("$.titulo_pauta").value(TITULO))
                .andExpect(jsonPath("$.documento").value(DOCUMENTO_VALIDO))
                .andExpect(jsonPath("$.escolha_voto").value("SIM"))
                .andExpect(jsonPath("$.started_at").exists())
                .andExpect(jsonPath("$.expires_at").exists());

        verify(service).votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM);
    }

    @Test
    @DisplayName("Should reject with 400 when id_sessao is missing")
    void save_shouldReject_whenIdSessaoIsMissing() throws Exception {
        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("idSessao"))
                .andExpect(jsonPath("$.field_errors[0].message").value("O id da Sessão é obrigatório"));

        verify(service, never()).votar(anyLong(), anyString(), any(Voto.Escolha.class));
    }

    @Test
    @DisplayName("Should reject with 400 when the documento is not a valid CPF")
    void save_shouldReject_whenDocumentoIsInvalidCpf() throws Exception {
        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_INVALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("documento"))
                .andExpect(jsonPath("$.field_errors[0].message").value("invalid Brazilian individual taxpayer registry number (CPF)"));

        verify(service, never()).votar(anyLong(), anyString(), any(Voto.Escolha.class));
    }

    @Test
    @DisplayName("Should reject with 400 when the documento is missing")
    void save_shouldReject_whenDocumentoIsMissing() throws Exception {
        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "escolha_voto": "SIM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("documento"))
                .andExpect(jsonPath("$.field_errors[0].message").value("O documento é obrigatório"));

        verify(service, never()).votar(anyLong(), anyString(), any(Voto.Escolha.class));
    }

    @Test
    @DisplayName("Should reject with 400 when escolha_voto is missing")
    void save_shouldReject_whenEscolhaVotoIsMissing() throws Exception {
        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("escolhaVoto"))
                .andExpect(jsonPath("$.field_errors[0].message").value("A escolha do voto é obrigatória"));

        verify(service, never()).votar(anyLong(), anyString(), any(Voto.Escolha.class));
    }

    @Test
    @DisplayName("Should reject with 400 when escolha_voto is not SIM or NAO")
    void save_shouldReject_whenEscolhaVotoIsNotSimOrNao() throws Exception {
        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "TALVEZ"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição malformado ou em formato inválido"));

        verify(service, never()).votar(anyLong(), anyString(), any(Voto.Escolha.class));
    }

    @Test
    @DisplayName("Should reject with 400 when the sessao does not exist")
    void save_shouldReject_whenSessaoDoesNotExist() throws Exception {
        when(service.votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM))
                .thenThrow(new SessaoNotFoundException(1L));

        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sessão com id 1 não encontrada"));

        verify(service).votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM);
    }

    @Test
    @DisplayName("Should reject with 400 when the sessao is closed")
    void save_shouldReject_whenSessaoIsClosed() throws Exception {
        when(service.votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM))
                .thenThrow(new SessaoIsClosedException(1L));

        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A sessão 1 está fechada"));

        verify(service).votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM);
    }

    @Test
    @DisplayName("Should reject with 400 when the documento is rejected by the external validator")
    void save_shouldReject_whenDocumentoIsRejectedByExternalValidator() throws Exception {
        when(service.votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM))
                .thenThrow(new InvalidDocumentoException(DOCUMENTO_VALIDO));

        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Documento inválido: 52998224725"));

        verify(service).votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM);
    }

    @Test
    @DisplayName("Should reject with 409 when the documento already voted in the sessao")
    void save_shouldReject_whenDocumentoAlreadyVoted() throws Exception {
        when(service.votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM))
                .thenThrow(new DuplicatedVoteException(1L, DOCUMENTO_VALIDO));

        mockMvc.perform(post("/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_sessao": 1,
                                  "documento": "%s",
                                  "escolha_voto": "SIM"
                                }
                                """.formatted(DOCUMENTO_VALIDO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("O documento 52998224725 já votou na sessão 1"));

        verify(service).votar(1L, DOCUMENTO_VALIDO, Voto.Escolha.SIM);
    }

}
