package com.payments.ingestion.model.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Payment ID must be a valid UUID")
    private UUID paymentId;

    @NotBlank(message = "Debit account ID must not be blank")
    private String debitAccountId;

    @NotBlank(message = "Credit account ID must not be blank")
    private String creditAccountId;

    @NotNull @DecimalMin(value = "0.01",
            message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank @Size(min = 3, max = 3,
            message = "Currency must be a 3-character ISO code")
    private String currency;

    @Size(max = 35,
            message = "Reference must not exceed 35 characters")
    private String reference;

    @NotNull @PastOrPresent(
            message = "Timestamp must not be in the future")
    private Instant timestamp;
}
