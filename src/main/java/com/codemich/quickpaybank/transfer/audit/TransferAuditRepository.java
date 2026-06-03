package com.codemich.quickpaybank.transfer.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferAuditRepository extends JpaRepository<TransferAudit, Long> {

    List<TransferAudit> findByPayerIdOrPayeeIdOrderByCreatedAtDesc(Long payerId, Long payeeId);
}
