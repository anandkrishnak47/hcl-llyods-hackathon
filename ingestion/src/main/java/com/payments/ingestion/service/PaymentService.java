package com.payments.ingestion.service;

import com.payments.ingestion.exception.*;
import com.payments.ingestion.model.Account;
import com.payments.ingestion.model.ProcessedPayment;
import com.payments.ingestion.model.dto.AccountResponse;
import com.payments.ingestion.model.dto.PaymentEvent;
import com.payments.ingestion.model.dto.PaymentRequest;
import com.payments.ingestion.model.dto.PaymentResponse;
import com.payments.ingestion.repository.AccountRepository;
import com.payments.ingestion.repository.ProcessedPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.SendResult;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final AccountRepository accountRepository;
    private final ProcessedPaymentRepository processedPaymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private static final String TOPIC = "payments.submitted";

    public PaymentResponse submitPayment(PaymentRequest request) {

        log.info("Received payment request: paymentId={}",
                request.getPaymentId());

        // ── Step 1: Idempotency check ─────────────────────────────────────
        // If this paymentId was already submitted, reject immediately.
        // Protects against client retries submitting the same payment twice.
        if (processedPaymentRepository.existsByPaymentId(
                request.getPaymentId())) {
            log.warn("Duplicate paymentId detected: {}",
                    request.getPaymentId());
            throw new DuplicatePaymentException(
                    "Duplicate paymentId: " + request.getPaymentId());
        }

        if(request.getDebitAccountId().equals(request.getCreditAccountId())){
            log.warn("Debit and credit accounts are the same: {}",
                    request.getDebitAccountId());
            throw new DebitAndCreditAccountSameException(
                    "Debit and credit accounts cannot be the same: "
                            + request.getDebitAccountId());
        }

        // ── Step 2: Validate debit account exists ─────────────────────────
        Account debitAccount = accountRepository
                .findById(request.getDebitAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Debit account not found: "
                                + request.getDebitAccountId()));

        // ── Step 3: Validate credit account exists ────────────────────────
        Account creditAccount = accountRepository
                .findById(request.getCreditAccountId())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Credit account not found: "
                                + request.getCreditAccountId()));

        // ── Step 4: Validate debit account is ACTIVE ─────────────────────
        if (!"ACTIVE".equals(debitAccount.getStatus())) {
            log.warn("Debit account suspended: {}",
                    request.getDebitAccountId());
            throw new AccountSuspendedException(
                    "Account is suspended: "
                            + request.getDebitAccountId());
        }

        // ── Step 5: Validate credit account is ACTIVE ────────────────────
        if (!"ACTIVE".equals(creditAccount.getStatus())) {
            log.warn("Credit account suspended: {}",
                    request.getCreditAccountId());
            throw new AccountSuspendedException(
                    "Account is suspended: "
                            + request.getCreditAccountId());
        }

        // ── Step 6: Build the Kafka event ─────────────────────────────────
        PaymentEvent event = PaymentEvent.builder()
                .paymentId(request.getPaymentId())
                .debitAccountId(request.getDebitAccountId())
                .creditAccountId(request.getCreditAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .reference(request.getReference())
                .timestamp(request.getTimestamp())
                .submittedAt(Instant.now())
                .build();

        // ── Step 7: Publish to Kafka ──────────────────────────────────────
        // Key = debitAccountId ensures partition affinity —
        // all payments from the same account land on the same partition
        // and are processed in order by the consumer.
        publishToKafka(event);

        // ── Step 8: Record paymentId for future idempotency checks ────────
        // Persisted AFTER successful Kafka publish to avoid marking
        // a payment as submitted if the broker was unreachable.
        processedPaymentRepository.save(
                ProcessedPayment.builder()
                        .paymentId(request.getPaymentId())
                        .submittedAt(Instant.now())
                        .build());

        log.info("Payment submitted successfully: paymentId={}",
                request.getPaymentId());

        return PaymentResponse.builder()
                .paymentId(request.getPaymentId())
                .status("ACCEPTED")
                .submittedAt(Instant.now())
                .build();
    }

    private void publishToKafka(PaymentEvent event) {
        CompletableFuture<SendResult<String, PaymentEvent>> future =
                kafkaTemplate.send(
                        TOPIC,
                        event.getDebitAccountId(),  // partition key
                        event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka publish failed for paymentId={} error={}",
                        event.getPaymentId(), ex.getMessage());
                // Rethrow so the controller returns 503
                throw new KafkaPublishException(
                        "Failed to publish payment to Kafka: "
                                + ex.getMessage());
            }
            log.debug("Published paymentId={} to partition={}",
                    event.getPaymentId(),
                    result.getRecordMetadata().partition());
        });
    }

    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountId));

        return AccountResponse.builder()
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .currency(account.getCurrency())
                .openedDate(account.getOpenedDate())
                .build();
    }
}
