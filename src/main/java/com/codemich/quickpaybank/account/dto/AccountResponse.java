package com.codemich.quickpaybank.account.dto;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        Long customerId,
        String customerName,
        AccountType accountType,
        String accountTypeDescription,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCustomer().getId(),
                account.getCustomer().getName(),
                account.getAccountType(),
                account.getAccountType().getDescription(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
