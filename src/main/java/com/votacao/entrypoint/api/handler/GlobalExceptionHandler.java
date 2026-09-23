package com.votacao.entrypoint.api.handler;

import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.PautaNotFoundException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Converte erros de validação de entrada em resposta padronizada.
     *
     * @param ex exceção de validação
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "Erro de validação", request, fields);
    }

    /**
     * Trata payloads JSON inválidos ou malformados.
     *
     * @param ex exceção de leitura HTTP
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    /**
     * Trata sessão inexistente.
     *
     * @param ex exceção específica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(SessaoNotFoundException.class)
    public ResponseEntity<ApiError> handleSessaoNotFound(SessaoNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    /**
     * Trata tentativa de operação em sessão fechada.
     *
     * @param ex exceção específica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(SessaoIsClosedException.class)
    public ResponseEntity<ApiError> handleSessaoIsClosed(SessaoIsClosedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    /**
     * Trata tentativa de apuração em sessão ainda aberta.
     *
     * @param ex exceção específica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(SessaoIsOpenException.class)
    public ResponseEntity<ApiError> handleSessaoIsOpen(SessaoIsOpenException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    /**
     * Trata voto duplicado na mesma sessão.
     *
     * @param ex exceção específica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(DuplicatedVoteException.class)
    public ResponseEntity<ApiError> handleDuplicatedVote(DuplicatedVoteException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    /**
     * Trata duplicidade de pauta pelo título.
     *
     * @param ex exceção específica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(DuplicatedPautaException.class)
    public ResponseEntity<ApiError> handleDuplicatedPauta(DuplicatedPautaException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    /**
     * Trata pauta inexistente.
     *
     * @param ex exceção específica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(PautaNotFoundException.class)
    public ResponseEntity<ApiError> handlePautaNotFound(PautaNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    /**
     * Trata erros inesperados não capturados em outros handlers.
     *
     * @param ex exceção genérica
     * @param request requisição que gerou o problema
     * @return resposta JSON padronizada
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request, List.of());
    }

    /**
     * Constrói o corpo da resposta de erro em formato padronizado.
     *
     * @param status código HTTP
     * @param message mensagem da exceção
     * @param request requisição atual
     * @param fieldErrors erros por campo, quando houver
     * @return resposta HTTP com o payload do erro
     */
    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ApiError.FieldError> fieldErrors
    ) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

}
