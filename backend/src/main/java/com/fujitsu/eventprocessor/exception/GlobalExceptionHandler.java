package com.fujitsu.eventprocessor.exception;

import com.fujitsu.eventprocessor.config.CorrelationIdFilter;
import com.fujitsu.eventprocessor.domain.EventType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento centralizado de exceções. Todas as respostas de erro seguem o
 * padrão Problem Details (RFC 9457) com campos extras: timestamp, correlationId e errors.
 * <p>
 * Estende {@link ResponseEntityExceptionHandler} para que as exceções padrão do Spring MVC
 * mantenham o status HTTP correto (405 método não suportado, 415 Content-Type inválido,
 * 404 rota inexistente, etc.) em vez de caírem no tratamento genérico de erro 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ALLOWED_TYPES = Arrays.toString(EventType.values());

    // ---------------------------------------------------------------- erros de negócio

    @ExceptionHandler(DuplicateEventException.class)
    public ProblemDetail handleDuplicate(DuplicateEventException ex) {
        ProblemDetail problem = build(HttpStatus.CONFLICT, "Evento duplicado", ex.getMessage(), "duplicate-event");
        problem.setProperty("eventId", ex.getEventId());
        return problem;
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleNotFound(EventNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Evento não encontrado", ex.getMessage(), "event-not-found");
    }

    @ExceptionHandler(UnsupportedEventTypeException.class)
    public ProblemDetail handleUnsupportedType(UnsupportedEventTypeException ex) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, "Tipo de evento não suportado", ex.getMessage(), "unsupported-event-type");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = "Valor inválido '%s' para o parâmetro '%s'".formatted(ex.getValue(), ex.getName());
        if (ex.getRequiredType() == EventType.class) {
            detail += ". Valores aceitos: " + ALLOWED_TYPES;
        }
        return build(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detail, "invalid-parameter");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado ao processar a requisição", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Informe o correlationId ao suporte.", "internal-error");
    }

    // ------------------------------------------- exceções do Spring MVC com mensagem própria

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        log.warn("Requisição inválida: {}", errors);
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "Dados inválidos",
                "Um ou mais campos são inválidos", "validation-error");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("Corpo da requisição ilegível: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = build(HttpStatus.BAD_REQUEST, "Requisição malformada",
                "O corpo da requisição é inválido. Verifique o formato JSON, o timestamp (ISO-8601, ex.: 2026-05-20T10:00:00) "
                        + "e o eventType (valores aceitos: " + ALLOWED_TYPES + ")",
                "malformed-request");
        return ResponseEntity.badRequest().headers(headers).body(problem);
    }

    /**
     * Ponto único por onde passam as demais exceções do Spring MVC (405, 415, 404...):
     * mantém o status e a mensagem padrão e acrescenta timestamp e correlationId.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.warn("Requisição rejeitada com status {}: {}", statusCode.value(), ex.getMessage());
        // O corpo (ProblemDetail) é montado pelo super a partir da própria exceção
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            enrich(problem);
        }
        return response;
    }

    // ---------------------------------------------------------------- utilitários

    private ProblemDetail build(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:problem-type:" + type));
        return enrich(problem);
    }

    private ProblemDetail enrich(ProblemDetail problem) {
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        return problem;
    }
}
