package com.codemich.quickpaybank.transfer.audit.dto;


import com.codemich.quickpaybank.transfer.audit.AuditStatus;
import com.codemich.quickpaybank.transfer.audit.TransferAudit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferAuditResponse(
        Long id,
        Long payerId,
        Long payeeId,
        BigDecimal amount,
        AuditStatus status,
        String reason,
        LocalDateTime createdAt
) {
    public static TransferAuditResponse from(TransferAudit audit) {
        return new TransferAuditResponse(
                audit.getId(),
                audit.getPayerId(),
                audit.getPayeeId(),
                audit.getAmount(),
                audit.getStatus(),
                audit.getReason(),
                audit.getCreatedAt()
        );
    }
}
