package com.payments.ingestion.exception;

import java.util.UUID;

public class DuplicatePaymentException extends RuntimeException {

    private final String paymentId;

    public DuplicatePaymentException(String paymentId) {
        super("Duplicate paymentId: " + paymentId);
        this.paymentId = paymentId;
    }

    public String getPaymentId() {
        return paymentId;
    }
}