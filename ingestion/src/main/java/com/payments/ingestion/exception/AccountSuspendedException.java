package com.payments.ingestion.exception;

public class AccountSuspendedException extends RuntimeException {

    private final String accountId;

    public AccountSuspendedException(String accountId) {
        super("Account is suspended: " + accountId);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
