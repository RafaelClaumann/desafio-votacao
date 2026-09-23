package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.service.SessaoService;
import com.votacao.application.service.VotoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    @DisplayName("Should create the sessao when pauta_id is informed")
    void save_shouldCreateSessao_whenPautaIdIsInformed() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Pauta pauta = new Pauta(1L, "Reforma estatutária do capítulo quatro", 10L);
        Sessao saved = new Sessao(1L, pauta, now, now.plusMinutes(10));

        when(sessaoService.saveSessao(1L)).thenReturn(saved);
        when(mapper.toDTO(any(Sessao.class)))
                .thenReturn(new SessaoResponseDTO(1L, 1L, now, now.plusMinutes(10), true));

        mockMvc.perform(post("/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pauta_id": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        verify(sessaoService).saveSessao(1L);
    }

    @Test
    @DisplayName("Should reject with 400 when pauta_id is missing")
    void save_shouldReject_whenPautaIdIsMissing() throws Exception {
        mockMvc.perform(post("/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("pautaId"))
                .andExpect(jsonPath("$.field_errors[0].message").value("O id da Pauta é obrigatório"));

        verify(sessaoService, never()).saveSessao(any());
    }

}
