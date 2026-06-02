package com.codemich.quickpaybank.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import com.codemich.quickpaybank.account.AccountType;
import java.math.BigDecimal;

public record CreateAccountRequest(

        @NotNull(message = "ID do cliente é obrigatório")
        Long customerId,

        @NotNull(message = "Tipo de conta é obrigatório")
        AccountType accountType,

        @NotNull(message = "Saldo inicial é obrigatório")
        @PositiveOrZero(message = "Saldo inicial não pode ser negativo")
        BigDecimal initialBalance
) {}
