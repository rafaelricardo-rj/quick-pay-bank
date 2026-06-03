package com.codemich.quickpaybank.transfer.audit;

import com.codemich.quickpaybank.transfer.audit.dto.TransferAuditResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferAuditService {

    private final TransferAuditRepository auditRepository;

    // Participates in the main transaction — rolled back together if transfer fails
    @Transactional
    public void logSuccess(Long payerId, Long payeeId, BigDecimal amount) {
        auditRepository.save(TransferAudit.builder()
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .status(AuditStatus.SUCCESS)
                .build());
    }

    // Independent transaction — commits even when the main transaction rolls back
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(Long payerId, Long payeeId, BigDecimal amount, String reason) {
        auditRepository.save(TransferAudit.builder()
                .payerId(payerId)
                .payeeId(payeeId)
                .amount(amount)
                .status(AuditStatus.FAILED)
                .reason(reason)
                .build());
    }

    @Transactional(readOnly = true)
    public List<TransferAuditResponse> findByAccountId(Long accountId) {
        return auditRepository.findByPayerIdOrPayeeIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(TransferAuditResponse::from)
                .toList();
    }
}
