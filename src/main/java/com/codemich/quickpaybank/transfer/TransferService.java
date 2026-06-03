package com.codemich.quickpaybank.transfer;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountRepository;
import com.codemich.quickpaybank.account.AccountType;
import com.codemich.quickpaybank.notification.NotificationService;
import com.codemich.quickpaybank.shared.exception.BusinessException;
import com.codemich.quickpaybank.shared.exception.InsufficientFundsException;
import com.codemich.quickpaybank.shared.exception.ResourceNotFoundException;
import com.codemich.quickpaybank.transfer.dto.TransferRequest;
import com.codemich.quickpaybank.transfer.dto.TransferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {

    private static final BigDecimal SAVINGS_PER_TRANSFER_LIMIT = new BigDecimal("2000.00");
    private static final BigDecimal SAVINGS_DAILY_LIMIT = new BigDecimal("5000.00");

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final NotificationService notificationService;

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.payerId().equals(request.payeeId())) {
            throw new BusinessException("Conta pagadora e recebedora não podem ser iguais");
        }

        // Lock in ascending ID order to prevent deadlock under concurrent requests
        Long firstId = Math.min(request.payerId(), request.payeeId());
        Long secondId = Math.max(request.payerId(), request.payeeId());

        Account first = accountRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + firstId));
        Account second = accountRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + secondId));

        Account payer = request.payerId().equals(firstId) ? first : second;
        Account payee = request.payerId().equals(firstId) ? second : first;

        validateTransferRules(payer, request.amount());

        payer.setBalance(payer.getBalance().subtract(request.amount()));
        payee.setBalance(payee.getBalance().add(request.amount()));

        Transfer transfer = transferRepository.save(Transfer.builder()
                .payer(payer)
                .payee(payee)
                .amount(request.amount())
                .status(TransferStatus.COMPLETED)
                .build());

        // Resolve customer data inside the transaction to avoid LazyInitializationException in async thread
        String payeeEmail = payee.getCustomer().getEmail();
        String payerName = payer.getCustomer().getName();

        notificationService.notify(
                payeeEmail,
                String.format("Transferência de R$ %.2f recebida de %s", request.amount(), payerName)
        );

        return TransferResponse.from(transfer);
    }

    @Transactional(readOnly = true)
    public TransferResponse findById(Long id) {
        return transferRepository.findById(id)
                .map(TransferResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transferência não encontrada: " + id));
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> findByAccountId(Long accountId) {
        return transferRepository.findByPayerIdOrPayeeIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(TransferResponse::from)
                .toList();
    }

    private void validateTransferRules(Account payer, BigDecimal amount) {
        if (payer.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente. Disponível: R$ " + payer.getBalance());
        }

        if (payer.getAccountType() == AccountType.SAVINGS) {
            if (amount.compareTo(SAVINGS_PER_TRANSFER_LIMIT) > 0) {
                throw new BusinessException(
                        "Conta Poupança: limite por transferência é R$ 2.000,00. Valor solicitado: R$ " + amount
                );
            }

            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            BigDecimal dailyTotal = transferRepository.findDailyOutgoingAmount(
                    payer.getId(), TransferStatus.COMPLETED, startOfDay, endOfDay
            );

            if (dailyTotal.add(amount).compareTo(SAVINGS_DAILY_LIMIT) > 0) {
                BigDecimal remaining = SAVINGS_DAILY_LIMIT.subtract(dailyTotal);
                throw new BusinessException(
                        "Conta Poupança: limite diário de R$ 5.000,00 excedido. Disponível hoje: R$ " + remaining
                );
            }
        }
    }
}
