package com.codemich.quickpaybank.account;

public enum AccountType {
    CHECKING("Conta Corrente"),
    SAVINGS("Conta Poupança");

    private final String description;

    AccountType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}