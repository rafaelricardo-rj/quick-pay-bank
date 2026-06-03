package com.codemich.quickpaybank.transfer;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountRepository;
import com.codemich.quickpaybank.account.AccountType;
import com.codemich.quickpaybank.notification.NotificationService;
import com.codemich.quickpaybank.shared.exception.BusinessException;
import com.codemich.quickpaybank.shared.exception.InsufficientFundsException;
import com.codemich.quickpaybank.shared.exception.ResourceNotFoundException;
import com.codemich.quickpaybank.transfer.audit.TransferAuditService;
import com.codemich.quickpaybank.transfer.dto.TransferRequest;
import com.codemich.quickpaybank.transfer.dto.TransferResponse;
import com.codemich.quickpaybank.transfer.validation.TransferValidationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final NotificationService notificationService;
    private final TransferAuditService auditService;
    private final Map<AccountType, TransferValidationStrategy> validationStrategies;

    public TransferService(AccountRepository accountRepository,
                           TransferRepository transferRepository,
                           NotificationService notificationService,
                           TransferAuditService auditService,
                           List<TransferValidationStrategy> strategies) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.validationStrategies = strategies.stream()
                .collect(Collectors.toMap(TransferValidationStrategy::supports, Function.identity()));
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {

        if (request.payerId().equals(request.payeeId())) {
            throw new BusinessException("Conta pagadora e recebedora não podem ser iguais");
        }

        try {
            // Lock in ascending ID order to prevent deadlock under concurrent requests
            Long firstId = Math.min(request.payerId(), request.payeeId());
            Long secondId = Math.max(request.payerId(), request.payeeId());

            Account first = accountRepository.findByIdWithLock(firstId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + firstId));
            Account second = accountRepository.findByIdWithLock(secondId)
                    .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada: " + secondId));

            Account payer = request.payerId().equals(firstId) ? first : second;
            Account payee = request.payerId().equals(firstId) ? second : first;

            if (payer.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException("Saldo insuficiente. Disponível: R$ " + payer.getBalance());
            }

            validationStrategies.get(payer.getAccountType()).validate(payer, request.amount());

            payer.setBalance(payer.getBalance().subtract(request.amount()));
            payee.setBalance(payee.getBalance().add(request.amount()));

            Transfer transfer = transferRepository.save(Transfer.builder()
                    .payer(payer)
                    .payee(payee)
                    .amount(request.amount())
                    .build());

            // Resolve customer data inside the transaction to avoid LazyInitializationException in async thread
            String payeeEmail = payee.getCustomer().getEmail();
            String payerName = payer.getCustomer().getName();

            notificationService.notify(
                    payeeEmail,
                    String.format("Transferência de R$ %.2f recebida de %s", request.amount(), payerName)
            );

            auditService.logSuccess(request.payerId(), request.payeeId(), request.amount());

            return TransferResponse.from(transfer);

        } catch (Exception e) {
            // logFailure uses REQUIRES_NEW — commits independently even though this transaction rolls back
            auditService.logFailure(request.payerId(), request.payeeId(), request.amount(), e.getMessage());
            throw e;
        }
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
}
