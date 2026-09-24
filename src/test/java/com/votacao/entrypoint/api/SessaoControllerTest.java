package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.service.SessaoService;
import com.votacao.application.service.VotoService;
import com.votacao.application.service.query.SessaoComStatus;
import com.votacao.entrypoint.api.dto.SessaoResponseDTO;
import com.votacao.entrypoint.mapper.SessaoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessaoController.class)
@DisplayName("SessaoController — POST /sessoes")
class SessaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessaoService sessaoService;

    @MockitoBean
    private VotoService votoService;

    @MockitoBean
    private SessaoMapper mapper;

    @Test
    @DisplayName("Should create the sessao when id_pauta is informed")
    void save_shouldCreateSessao_whenPautaIdIsInformed() throws Exception {
        String titulo = "Reforma estatutária do capítulo quatro";
        LocalDateTime now = LocalDateTime.now();
        Pauta pauta = new Pauta(1L, titulo, 10L);
        Sessao saved = new Sessao(1L, pauta, now, now.plusMinutes(10));

        when(sessaoService.saveSessao(1L)).thenReturn(saved);
        when(mapper.toDTO(any(SessaoComStatus.class)))
                .thenReturn(new SessaoResponseDTO(1L, 1L, titulo, now, now.plusMinutes(10), true));

        mockMvc.perform(post("/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_pauta": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        verify(sessaoService).saveSessao(1L);
    }

    @Test
    @DisplayName("Should reject with 409 when the pauta already has a session")
    void save_shouldReject_whenPautaAlreadyHasSession() throws Exception {
        when(sessaoService.saveSessao(1L)).thenThrow(new DuplicatedSessaoException(1L));

        mockMvc.perform(post("/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_pauta": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Já existe uma sessão para a pauta: 1"));

        verify(sessaoService).saveSessao(1L);
    }

    @Test
    @DisplayName("Should reject with 400 when id_pauta is missing")
    void save_shouldReject_whenPautaIdIsMissing() throws Exception {
        mockMvc.perform(post("/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("idPauta"))
                .andExpect(jsonPath("$.field_errors[0].message").value("O id da Pauta é obrigatório"));

        verify(sessaoService, never()).saveSessao(any());
    }

    @Test
    @DisplayName("Should list sessions")
    void fetch_shouldListSessions() throws Exception {
        String titulo = "Reforma estatutária do capítulo quatro";
        LocalDateTime now = LocalDateTime.now();
        Pauta pauta = new Pauta(1L, titulo, 10L);
        Sessao open = new Sessao(1L, pauta, now.minusMinutes(5), now.plusMinutes(5));

        when(sessaoService.getSessoesComStatus())
                .thenReturn(List.of(new SessaoComStatus(open, true)));
        when(mapper.toDTOList(anyList()))
                .thenReturn(List.of(new SessaoResponseDTO(1L, 1L, titulo, now.minusMinutes(5), now.plusMinutes(5), true)));

        mockMvc.perform(get("/sessoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].is_open").value(true));

        verify(sessaoService).getSessoesComStatus();
    }

}
