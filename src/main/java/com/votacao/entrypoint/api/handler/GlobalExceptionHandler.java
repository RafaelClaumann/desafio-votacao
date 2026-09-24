package com.votacao.entrypoint.api.handler;

import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.model.exception.PautaNotFoundException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import com.votacao.entrypoint.client.exception.HttpIntegrationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MDC_CORRELATION_ID_KEY = "correlationId";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiError.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, "Erro de validação", request, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.info("Corpo da requisição ilegível em {}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição malformado ou em formato inválido",
                request,
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "Recurso não encontrado", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não suportado para este recurso",
                request,
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Parâmetro de caminho com tipo inválido",
                request,
                List.of()
        );
    }

    @ExceptionHandler(PautaNotFoundException.class)
    public ResponseEntity<ApiError> handlePautaNotFound(PautaNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicatedPautaException.class)
    public ResponseEntity<ApiError> handleDuplicatedPauta(DuplicatedPautaException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({SessaoNotFoundException.class, SessaoIsClosedException.class, SessaoIsOpenException.class})
    public ResponseEntity<ApiError> handleSessaoBadRequest(RuntimeException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicatedSessaoException.class)
    public ResponseEntity<ApiError> handleDuplicatedSessao(DuplicatedSessaoException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicatedVoteException.class)
    public ResponseEntity<ApiError> handleDuplicatedVote(DuplicatedVoteException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(InvalidDocumentoException.class)
    public ResponseEntity<ApiError> handleInvalidDocumento(InvalidDocumentoException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(HttpIntegrationException.class)
    public ResponseEntity<ApiError> handleHttpIntegration(HttpIntegrationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request, List.of());
    }

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
                MDC.get(MDC_CORRELATION_ID_KEY),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
