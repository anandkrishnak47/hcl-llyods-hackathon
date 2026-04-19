package com.payments.ingestion.controller;

import com.payments.ingestion.model.dto.AccountResponse;
import com.payments.ingestion.model.dto.PaymentRequest;
import com.payments.ingestion.model.dto.PaymentResponse;
import com.payments.ingestion.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // ── POST /api/payments ────────────────────────────────────────────────
    // Main payment submission endpoint.
    // @Valid triggers Bean Validation on PaymentRequest —
    // any constraint violation is caught by GlobalExceptionHandler
    // and returned as a 400 with the full violations list.
    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> submitPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info("POST /api/payments paymentId={}", request.getPaymentId());

        PaymentResponse response = paymentService.submitPayment(request);

        return ResponseEntity
                .accepted()   // 202
                .body(response);
    }

    // ── GET /api/accounts/{accountId} ─────────────────────────────────────
    // Returns account details from H2.
    // accountId format contains slashes e.g. "20-15-88/43917265"
    //
    // IMPORTANT: Use URL-encoded form with %2F for slashes:
    //   http://localhost:8081/api/accounts/20-15-88%2F61082934
    //
    // The .+ regex pattern matches the full encoded account ID.
    // Spring decodes %2F → / with allow-decoded-slash: true in application.yml.
    @GetMapping("/accounts/{accountId:.+}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable String accountId) {

        log.info("GET /api/accounts/{}", accountId);

        AccountResponse response = paymentService.getAccount(accountId);

        return ResponseEntity.ok(response);
    }

    // ── Alternative: GET /api/accounts?id={accountId} ───────────────────────
    // Returns account details using query parameter instead of path variable.
    // Useful if you can't URL-encode the account ID in the path.
    // Example: http://localhost:8081/api/accounts?id=20-15-88/61082934
    @GetMapping("/accounts")
    public ResponseEntity<AccountResponse> getAccountByQuery(
            @RequestParam(value = "id", name = "id") String accountId) {

        log.info("GET /api/accounts?id={}", accountId);

        AccountResponse response = paymentService.getAccount(accountId);

        return ResponseEntity.ok(response);
    }
}
