package com.codemich.quickpaybank.transfer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull(message = "ID da conta pagadora é obrigatório")
        Long payerId,

        @NotNull(message = "ID da conta recebedora é obrigatório")
        Long payeeId,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que zero")
        BigDecimal amount
) {}
