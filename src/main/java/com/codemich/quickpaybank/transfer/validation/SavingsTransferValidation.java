package com.codemich.quickpaybank.transfer.validation;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountType;
import com.codemich.quickpaybank.shared.exception.BusinessException;
import com.codemich.quickpaybank.transfer.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SavingsTransferValidation implements TransferValidationStrategy {

    private static final BigDecimal PER_TRANSFER_LIMIT = new BigDecimal("2000.00");
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("5000.00");

    private final TransferRepository transferRepository;

    @Override
    public AccountType supports() {
        return AccountType.SAVINGS;
    }

    @Override
    public void validate(Account payer, BigDecimal amount) {
        if (amount.compareTo(PER_TRANSFER_LIMIT) > 0) {
            throw new BusinessException(
                    "Conta Poupança: limite por transferência é R$ 2.000,00. Valor solicitado: R$ " + amount
            );
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        BigDecimal dailyTotal = transferRepository.findDailyOutgoingAmount(payer.getId(), startOfDay, endOfDay);

        if (dailyTotal.add(amount).compareTo(DAILY_LIMIT) > 0) {
            BigDecimal remaining = DAILY_LIMIT.subtract(dailyTotal);
            throw new BusinessException(
                    "Conta Poupança: limite diário de R$ 5.000,00 excedido. Disponível hoje: R$ " + remaining
            );
        }
    }
}
