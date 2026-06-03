package com.codemich.quickpaybank.transfer;

import com.codemich.quickpaybank.account.Account;
import com.codemich.quickpaybank.account.AccountRepository;
import com.codemich.quickpaybank.account.AccountType;
import com.codemich.quickpaybank.account.Customer;
import com.codemich.quickpaybank.notification.NotificationService;
import com.codemich.quickpaybank.shared.exception.BusinessException;
import com.codemich.quickpaybank.shared.exception.InsufficientFundsException;
import com.codemich.quickpaybank.shared.exception.ResourceNotFoundException;
import com.codemich.quickpaybank.transfer.audit.TransferAuditService;
import com.codemich.quickpaybank.transfer.dto.TransferRequest;
import com.codemich.quickpaybank.transfer.validation.CheckingTransferValidation;
import com.codemich.quickpaybank.transfer.validation.SavingsTransferValidation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TransferAuditService auditService;

    private TransferService transferService;

    private Customer customer;
    private Account checkingAccount;
    private Account savingsAccount;

    @BeforeEach
    void setUp() {
        // Real strategy instances — they are part of the business logic being tested
        var strategies = List.of(
                new CheckingTransferValidation(),
                new SavingsTransferValidation(transferRepository)
        );

        transferService = new TransferService(
                accountRepository, transferRepository, notificationService, auditService, strategies
        );

        customer = Customer.builder()
                .id(1L)
                .name("Ana Silva")
                .email("ana@email.com")
                .document("11111111111")
                .build();

        checkingAccount = Account.builder()
                .id(1L)
                .customer(customer)
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("10000.00"))
                .build();

        savingsAccount = Account.builder()
                .id(2L)
                .customer(customer)
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    void transfer_checkingWithSufficientBalance_shouldSucceedAndNotify() {
        var request = new TransferRequest(1L, 2L, new BigDecimal("500.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(savingsAccount));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = transferService.transfer(request);

        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("9500.00");
        assertThat(savingsAccount.getBalance()).isEqualByComparingTo("5500.00");
        assertThat(response.amount()).isEqualByComparingTo("500.00");
        verify(notificationService).notify(anyString(), anyString());
        verify(auditService).logSuccess(1L, 2L, new BigDecimal("500.00"));
        verify(auditService, never()).logFailure(any(), any(), any(), any());
    }

    @Test
    void transfer_withInsufficientBalance_shouldThrowAndLogFailure() {
        var request = new TransferRequest(1L, 2L, new BigDecimal("15000.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(savingsAccount));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(transferRepository, never()).save(any());
        verify(notificationService, never()).notify(anyString(), anyString());
        verify(auditService).logFailure(eq(1L), eq(2L), eq(new BigDecimal("15000.00")), anyString());
        verify(auditService, never()).logSuccess(any(), any(), any());
    }

    @Test
    void transfer_savingsExceedingPerTransferLimit_shouldThrowAndLogFailure() {
        var request = new TransferRequest(2L, 1L, new BigDecimal("2500.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(savingsAccount));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2.000,00");

        verify(transferRepository, never()).save(any());
        verify(auditService).logFailure(eq(2L), eq(1L), eq(new BigDecimal("2500.00")), anyString());
    }

    @Test
    void transfer_savingsExceedingDailyLimit_shouldThrowAndLogFailure() {
        var request = new TransferRequest(2L, 1L, new BigDecimal("1500.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(savingsAccount));
        when(transferRepository.findDailyOutgoingAmount(eq(2L), any(), any()))
                .thenReturn(new BigDecimal("4000.00"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("5.000,00");

        verify(transferRepository, never()).save(any());
        verify(auditService).logFailure(eq(2L), eq(1L), eq(new BigDecimal("1500.00")), anyString());
    }

    @Test
    void transfer_toSameAccount_shouldThrowBusinessException() {
        var request = new TransferRequest(1L, 1L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("iguais");

        verify(accountRepository, never()).findByIdWithLock(any());
        verify(auditService, never()).logSuccess(any(), any(), any());
        verify(auditService, never()).logFailure(any(), any(), any(), any());
    }

    @Test
    void transfer_withNonExistentAccount_shouldThrowAndLogFailure() {
        var request = new TransferRequest(1L, 99L, new BigDecimal("100.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(auditService).logFailure(eq(1L), eq(99L), eq(new BigDecimal("100.00")), anyString());
    }

    @Test
    void transfer_notificationFailure_shouldNotAffectTransfer() {
        var request = new TransferRequest(1L, 2L, new BigDecimal("200.00"));

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(checkingAccount));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(savingsAccount));
        when(transferRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(notificationService).notify(anyString(), anyString());

        assertThatCode(() -> transferService.transfer(request)).doesNotThrowAnyException();
        assertThat(checkingAccount.getBalance()).isEqualByComparingTo("9800.00");
        verify(auditService).logSuccess(1L, 2L, new BigDecimal("200.00"));
    }
}
