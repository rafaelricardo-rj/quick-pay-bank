package com.codemich.quickpaybank.transfer.dto;

import com.codemich.quickpaybank.transfer.Transfer;
import com.codemich.quickpaybank.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long payerId,
        String payerName,
        Long payeeId,
        String payeeName,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt
) {
    public static TransferResponse from(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getPayer().getId(),
                transfer.getPayer().getCustomer().getName(),
                transfer.getPayee().getId(),
                transfer.getPayee().getCustomer().getName(),
                transfer.getAmount(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }
}
