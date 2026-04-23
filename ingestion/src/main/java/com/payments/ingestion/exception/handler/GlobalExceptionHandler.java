package com.payments.ingestion.exception.handler;

import com.payments.ingestion.exception.*;
import com.payments.ingestion.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        List<Map<String, String>> violations = ex
                .getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toViolation)
                .toList();

        log.warn("Validation failed path={} violations={}",
                req.getRequestURI(), violations);

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(
                        400,
                        "Validation failed",
                        req.getRequestURI(),
                        violations));
    }

    // ── 404 Account not found ─────────────────────────────────────────────
    // Thrown by PaymentService when debitAccountId or
    // creditAccountId does not exist in the accounts table.
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(
            AccountNotFoundException ex,
            HttpServletRequest req) {

        log.warn("Account not found accountId={} path={}",
                ex.getAccountId(), req.getRequestURI());

        return ResponseEntity
                .status(404)
                .body(new ErrorResponse(
                        404,
                        ex.getMessage(),
                        req.getRequestURI()));
    }

    @ExceptionHandler(DebitAndCreditAccountSameException.class)
    public ResponseEntity<ErrorResponse> handleDebitAndCreditAccountSame(
            DebitAndCreditAccountSameException ex,
            HttpServletRequest req) {

        log.warn("Account cannot be same for Debit and Credit, accountId={} path={}",
                ex.getAccountId(), req.getRequestURI());

        return ResponseEntity
                .status(409)
                .body(new ErrorResponse(
                        409,
                        ex.getMessage(),
                        req.getRequestURI()));
    }

    // ── 409 Duplicate payment ─────────────────────────────────────────────
    // Thrown by PaymentService when the incoming paymentId
    // already exists in the processed_payments table.
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicatePaymentException ex,
            HttpServletRequest req) {

        log.warn("Duplicate paymentId={} path={}",
                ex.getPaymentId(), req.getRequestURI());

        return ResponseEntity
                .status(409)
                .body(new ErrorResponse(
                        409,
                        ex.getMessage(),
                        req.getRequestURI()));
    }

    // ── 422 Account suspended ─────────────────────────────────────────────
    // Thrown by PaymentService when debitAccountId or
    // creditAccountId has status SUSPENDED.
    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ErrorResponse> handleSuspended(
            AccountSuspendedException ex,
            HttpServletRequest req) {

        log.warn("Account suspended accountId={} path={}",
                ex.getAccountId(), req.getRequestURI());

        return ResponseEntity
                .status(422)
                .body(new ErrorResponse(
                        422,
                        ex.getMessage(),
                        req.getRequestURI()));
    }

    // ── 503 Kafka publish failure ─────────────────────────────────────────
    // Thrown by PaymentService when KafkaTemplate.send() fails
    // after all producer retries are exhausted.
    @ExceptionHandler(KafkaPublishException.class)
    public ResponseEntity<ErrorResponse> handleKafkaFailure(
            KafkaPublishException ex,
            HttpServletRequest req) {

        log.error("Kafka publish failed path={} error={}",
                req.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(503)
                .body(new ErrorResponse(
                        503,
                        "Payment accepted but could not be queued. "
                                + "Please retry.",
                        req.getRequestURI()));
    }

    // ── 500 Unexpected fallback ───────────────────────────────────────────
    // Catches anything not handled above.
    // Never expose internal exception messages to the client —
    // log the detail server-side only.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest req) {

        log.error("Unexpected error path={} error={}",
                req.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(500)
                .body(new ErrorResponse(
                        500,
                        "An unexpected error occurred.",
                        req.getRequestURI()));
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Map<String, String> toViolation(FieldError error) {
        return Map.of(
                "field",   error.getField(),
                "message", error.getDefaultMessage() != null
                        ? error.getDefaultMessage()
                        : "Invalid value");
    }
}
