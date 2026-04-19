package com.payments.ingestion.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private UUID paymentId;
    private String debitAccountId;
    private String creditAccountId;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private Instant timestamp;
    private Instant submittedAt;
}
