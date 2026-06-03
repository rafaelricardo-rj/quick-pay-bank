package com.codemich.quickpaybank.transfer.validation;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CheckingTransferValidation implements TransferValidationStrategy {

    @Override
    public AccountType supports() {
        return AccountType.CHECKING;
    }

    @Override
    public void validate(Account payer, BigDecimal amount) {
        // Conta corrente não possui restrições além do saldo disponível
    }
}
