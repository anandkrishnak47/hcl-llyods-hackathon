package com.payments.ingestion.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedPayment {

    @Id
    private UUID paymentId;

    private Instant submittedAt;
}
