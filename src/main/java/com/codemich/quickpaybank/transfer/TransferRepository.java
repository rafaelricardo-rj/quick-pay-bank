package com.codemich.quickpaybank.transfer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transfer t
            WHERE t.payer.id = :payerId
              AND t.createdAt >= :startOfDay
              AND t.createdAt < :endOfDay
            """)
    BigDecimal findDailyOutgoingAmount(
            @Param("payerId") Long payerId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    List<Transfer> findByPayerIdOrPayeeIdOrderByCreatedAtDesc(Long payerId, Long payeeId);
}
