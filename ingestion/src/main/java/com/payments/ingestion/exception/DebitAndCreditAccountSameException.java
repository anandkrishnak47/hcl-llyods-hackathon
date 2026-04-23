package com.payments.ingestion.exception;

public class DebitAndCreditAccountSameException extends RuntimeException {
    private final String accountId;

    public DebitAndCreditAccountSameException(String accountId) {
        super("Account same: " + accountId);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
