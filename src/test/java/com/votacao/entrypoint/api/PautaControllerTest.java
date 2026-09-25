package com.votacao.entrypoint.api;

import com.votacao.application.model.Pauta;
import com.votacao.application.service.PautaService;
import com.votacao.entrypoint.api.dto.PautaRequestDTO;
import com.votacao.entrypoint.api.dto.PautaResponseDTO;
import com.votacao.entrypoint.mapper.PautaMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PautaController.class)
@DisplayName("PautaController — POST /pautas")
class PautaControllerTest {

    private static final String TITULO = "Reforma estatutária do capítulo quatro";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PautaService service;

    @MockitoBean
    private PautaMapper mapper;

    @Test
    @DisplayName("Should create the pauta when the duration is positive")
    void save_shouldCreatePauta_whenDurationIsPositive() throws Exception {
        Pauta domain = new Pauta(null, TITULO, 10L);
        Pauta saved = new Pauta(1L, TITULO, 10L);

        when(mapper.toDomain(any(PautaRequestDTO.class))).thenReturn(domain);
        when(service.savePauta(any())).thenReturn(saved);
        when(mapper.toResponse(any())).thenReturn(new PautaResponseDTO(1L, TITULO, 10L));

        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(10L)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        verify(service).savePauta(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -5L})
    @DisplayName("Should reject with 400 when the duration is less than or equal to zero")
    void save_shouldReject_whenDurationIsNotPositive(long tempoVotacaoMinutos) throws Exception {
        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(tempoVotacaoMinutos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("tempoVotacaoMinutos"))
                .andExpect(jsonPath("$.field_errors[0].message").value("O tempo de votação deve ser maior que zero"));

        verify(service, never()).savePauta(any());
    }

    @ParameterizedTest
    @ValueSource(longs = {43201L, Long.MAX_VALUE})
    @DisplayName("Should reject with 400 when the duration exceeds the upper limit")
    void save_shouldReject_whenDurationExceedsUpperLimit(long tempoVotacaoMinutos) throws Exception {
        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody(tempoVotacaoMinutos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("tempoVotacaoMinutos"))
                .andExpect(jsonPath("$.field_errors[0].message")
                        .value("O tempo de votação não pode exceder 43200 minutos (30 dias)"));

        verify(service, never()).savePauta(any());
    }

    @Test
    @DisplayName("Should reject with 400 when the duration is missing")
    void save_shouldReject_whenDurationIsNull() throws Exception {
        mockMvc.perform(post("/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "%s"
                                }
                                """.formatted(TITULO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Erro de validação"))
                .andExpect(jsonPath("$.field_errors[0].field").value("tempoVotacaoMinutos"))
                .andExpect(jsonPath("$.field_errors[0].message").value("O tempo de votação é obrigatório"));

        verify(service, never()).savePauta(any());
    }

    private String jsonBody(Long tempoVotacaoMinutos) {
        return """
                {
                  "titulo": "%s",
                  "tempo_votacao_minutos": %d
                }
                """.formatted(TITULO, tempoVotacaoMinutos);
    }

}
