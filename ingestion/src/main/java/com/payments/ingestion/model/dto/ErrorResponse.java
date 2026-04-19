package com.payments.ingestion.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
                timezone = "UTC")
        Instant timestamp,

        int status,
        String error,
        String path,
        List<Map<String, String>> violations
) {
    // ── Convenience constructors ──────────────────────────────────────────

    // With violations — used for 400 validation errors
    public ErrorResponse(int status, String error,
                         String path,
                         List<Map<String, String>> violations) {
        this(Instant.now(), status, error, path, violations);
    }

    // Without violations — used for 404, 409, 422, 503
    public ErrorResponse(int status, String error, String path) {
        this(Instant.now(), status, error, path, List.of());
    }
}
