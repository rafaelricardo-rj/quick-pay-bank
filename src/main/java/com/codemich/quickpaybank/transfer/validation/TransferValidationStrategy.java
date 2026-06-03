package com.codemich.quickpaybank.transfer.validation;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountType;

import java.math.BigDecimal;

public interface TransferValidationStrategy {

    AccountType supports();

    void validate(Account payer, BigDecimal amount);
}
