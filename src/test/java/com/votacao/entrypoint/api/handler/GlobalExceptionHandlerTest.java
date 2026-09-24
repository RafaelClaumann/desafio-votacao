package com.votacao.entrypoint.api.handler;

import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.model.exception.PautaNotFoundException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import com.votacao.application.service.PautaService;
import com.votacao.application.service.SessaoService;
import com.votacao.application.service.VotoService;
import com.votacao.entrypoint.api.PautaController;
import com.votacao.entrypoint.api.SessaoController;
import com.votacao.entrypoint.api.VotoController;
import com.votacao.entrypoint.client.exception.HttpIntegrationException;
import com.votacao.entrypoint.mapper.PautaMapper;
import com.votacao.entrypoint.mapper.SessaoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PautaController.class, SessaoController.class, VotoController.class})
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private static final String TITULO = "Reforma estatutária do capítulo quatro";
    private static final String DOCUMENTO = "12345678909";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PautaService pautaService;

    @MockitoBean
    private PautaMapper pautaMapper;

    @MockitoBean
    private SessaoService sessaoService;

    @MockitoBean
    private VotoService votoService;

    @MockitoBean
    private SessaoMapper sessaoMapper;

    @Nested
    @DisplayName("Route and HTTP method")
    class RotaEMetodo {

        @Test
        @DisplayName("Should respond 404 with a stable message when the route does not exist")
        void handleNotFound_shouldRespond404_whenRouteDoesNotExist() throws Exception {
            mockMvc.perform(get("/nao-existe"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Recurso não encontrado"))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.path").value("/nao-existe"));
        }

        @Test
        @DisplayName("Should respond 405 with a stable message when the HTTP method is not supported")
        void handleMethodNotAllowed_shouldRespond405_whenMethodNotSupported() throws Exception {
            mockMvc.perform(put("/pautas"))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.message").value("Método HTTP não suportado para este recurso"));
        }

    }

    @Nested
    @DisplayName("Malformed request")
    class RequisicaoMalformada {

        @Test
        @DisplayName("Should respond 400 when a path parameter has an invalid type")
        void handleTypeMismatch_shouldRespond400_whenPathVariableHasInvalidType() throws Exception {
            mockMvc.perform(get("/sessoes/nao-numerico/resultado"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Parâmetro de caminho com tipo inválido"));
        }

        @Test
        @DisplayName("Should respond 400 with a stable message when the request body is unreadable")
        void handleUnreadable_shouldRespond400_withStableMessage() throws Exception {
            mockMvc.perform(post("/pautas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ \"titulo\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Corpo da requisição malformado ou em formato inválido"));
        }

    }

    @Nested
    @DisplayName("Input validation")
    class ValidacaoDeEntrada {

        @Test
        @DisplayName("Should respond 400 with field errors when bean validation fails")
        void handleValidation_shouldRespond400_whenBeanValidationFails() throws Exception {
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
        }

    }

    @Nested
    @DisplayName("Pauta business errors")
    class ErrosDePauta {

        @Test
        @DisplayName("Should respond 409 when the pauta title is duplicated")
        void handleDuplicatedPauta_shouldRespond409_whenTitleIsDuplicated() throws Exception {
            when(pautaService.savePauta(any())).thenThrow(new DuplicatedPautaException(TITULO));

            mockMvc.perform(post("/pautas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "titulo": "%s",
                                      "tempo_votacao_minutos": 10
                                    }
                                    """.formatted(TITULO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Já existe uma pauta com o título: " + TITULO));
        }

        @Test
        @DisplayName("Should respond 400 when the pauta does not exist")
        void handlePautaNotFound_shouldRespond400_whenPautaDoesNotExist() throws Exception {
            when(sessaoService.saveSessao(1L)).thenThrow(new PautaNotFoundException(1L));

            mockMvc.perform(post("/sessoes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pauta_id": 1
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Pauta com id 1 não encontrada"));
        }

    }

    @Nested
    @DisplayName("Sessao business errors")
    class ErrosDeSessao {

        @Test
        @DisplayName("Should respond 400 when the sessao does not exist")
        void handleSessaoNotFound_shouldRespond400_whenSessaoDoesNotExist() throws Exception {
            when(votoService.apurarVotosSessao(1L)).thenThrow(new SessaoNotFoundException(1L));

            mockMvc.perform(get("/sessoes/1/resultado"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Sessão com id 1 não encontrada"));
        }

        @Test
        @DisplayName("Should respond 400 when voting on a closed sessao")
        void handleSessaoIsClosed_shouldRespond400_whenSessaoIsClosed() throws Exception {
            when(votoService.votar(1L, DOCUMENTO, Voto.Escolha.SIM))
                    .thenThrow(new SessaoIsClosedException(1L));

            mockMvc.perform(post("/votos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(votoJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("A sessão 1 está fechada"));
        }

        @Test
        @DisplayName("Should respond 400 when apurating an open sessao")
        void handleSessaoIsOpen_shouldRespond400_whenSessaoIsOpen() throws Exception {
            when(votoService.apurarVotosSessao(1L)).thenThrow(new SessaoIsOpenException(1L));

            mockMvc.perform(get("/sessoes/1/resultado"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("A sessão 1 ainda está aberta"));
        }

        @Test
        @DisplayName("Should respond 409 when the pauta already has a sessao")
        void handleDuplicatedSessao_shouldRespond409_whenPautaAlreadyHasSessao() throws Exception {
            when(sessaoService.saveSessao(1L)).thenThrow(new DuplicatedSessaoException(1L));

            mockMvc.perform(post("/sessoes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "pauta_id": 1
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Já existe uma sessão para a pauta: 1"));
        }

    }

    @Nested
    @DisplayName("Voto business errors")
    class ErrosDeVoto {

        @Test
        @DisplayName("Should respond 409 when the documento already voted in the sessao")
        void handleDuplicatedVote_shouldRespond409_whenDocumentoAlreadyVoted() throws Exception {
            when(votoService.votar(1L, DOCUMENTO, Voto.Escolha.SIM))
                    .thenThrow(new DuplicatedVoteException(1L, DOCUMENTO));

            mockMvc.perform(post("/votos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(votoJson()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("O documento " + DOCUMENTO + " já votou na sessão 1"));
        }

        @Test
        @DisplayName("Should respond 400 when the documento is invalid")
        void handleInvalidDocumento_shouldRespond400_whenDocumentoIsInvalid() throws Exception {
            when(votoService.votar(1L, DOCUMENTO, Voto.Escolha.SIM))
                    .thenThrow(new InvalidDocumentoException(DOCUMENTO));

            mockMvc.perform(post("/votos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(votoJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Documento inválido: " + DOCUMENTO));
        }

        @Test
        @DisplayName("Should respond 503 when the external document validator fails")
        void handleHttpIntegration_shouldRespond503_whenExternalValidatorFails() throws Exception {
            when(votoService.votar(1L, DOCUMENTO, Voto.Escolha.SIM))
                    .thenThrow(new HttpIntegrationException(HttpStatus.SERVICE_UNAVAILABLE));

            mockMvc.perform(post("/votos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(votoJson()))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Falha na integração externa de validação de documento: 503"));
        }

    }

    @Nested
    @DisplayName("Generic error")
    class ErroGenerico {

        @Test
        @DisplayName("Should respond 500 with a stable message when an unexpected exception occurs")
        void handleGeneric_shouldRespond500_whenUnexpectedExceptionOccurs() throws Exception {
            when(pautaService.savePauta(any())).thenThrow(new IllegalStateException("boom"));

            mockMvc.perform(post("/pautas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "titulo": "%s",
                                      "tempo_votacao_minutos": 10
                                    }
                                    """.formatted(TITULO)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Erro interno do servidor"));
        }

    }

    private static String votoJson() {
        return """
                {
                  "id_sessao": 1,
                  "documento": "%s",
                  "escolha_voto": "SIM"
                }
                """.formatted(DOCUMENTO);
    }

}
