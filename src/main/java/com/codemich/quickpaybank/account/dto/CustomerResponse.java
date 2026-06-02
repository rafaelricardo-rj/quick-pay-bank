package com.codemich.quickpaybank.account.dto;

import com.codemich.quickpaybank.account.AccountType;
import com.codemich.quickpaybank.account.Customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        Long id,
        String name,
        String document,
        String email,
        List<AccountSummary> accounts,
        LocalDateTime createdAt
) {
    public record AccountSummary(Long id, AccountType accountType, String accountTypeDescription, BigDecimal balance) {}

    public static CustomerResponse from(Customer customer) {
        List<AccountSummary> accountSummaries = customer.getAccounts().stream()
                .map(a -> new AccountSummary(a.getId(), a.getAccountType(), a.getAccountType().getDescription(), a.getBalance()))
                .toList();
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getDocument(),
                customer.getEmail(),
                accountSummaries,
                customer.getCreatedAt()
        );
    }
}
