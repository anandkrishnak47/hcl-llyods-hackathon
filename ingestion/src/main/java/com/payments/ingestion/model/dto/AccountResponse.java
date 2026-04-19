package com.payments.ingestion.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {

    private String accountId;
    private String accountName;
    private String accountType;
    private String status;
    private String currency;
    private LocalDate openedDate;
}
