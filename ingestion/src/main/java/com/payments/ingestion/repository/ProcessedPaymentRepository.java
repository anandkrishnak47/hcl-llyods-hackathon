package com.payments.ingestion.repository;

import com.payments.ingestion.model.ProcessedPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedPaymentRepository extends JpaRepository<ProcessedPayment, UUID> {

    boolean existsByPaymentId(UUID paymentId);
}
