package com.codemich.quickpaybank.account.dto;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        AccountType accountType,
        String accountTypeDescription,
        BigDecimal balance,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountType(),
                account.getAccountType().getDescription(),
                account.getBalance(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
