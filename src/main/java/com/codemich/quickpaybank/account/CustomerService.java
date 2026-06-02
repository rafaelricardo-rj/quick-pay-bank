package com.codemich.quickpaybank.account;

import com.codemich.quickpaybank.account.dto.CreateAccountRequest;
import com.codemich.quickpaybank.account.dto.CreateCustomerRequest;
import com.codemich.quickpaybank.account.dto.CustomerResponse;
import com.codemich.quickpaybank.shared.exception.BusinessException;
import com.codemich.quickpaybank.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByDocument(request.document())) {
            throw new BusinessException("CPF já cadastrado: " + request.document());
        }
        if (customerRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado: " + request.email());
        }

        Customer customer = customerRepository.save(Customer.builder()
                .name(request.name())
                .document(request.document())
                .email(request.email())
                .build());

        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse addAccount(CreateAccountRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.customerId()));

        if (accountRepository.existsByCustomerIdAndAccountType(request.customerId(), request.accountType())) {
            throw new BusinessException("Cliente já possui uma " + request.accountType().getDescription());
        }

        accountRepository.save(Account.builder()
                .customer(customer)
                .accountType(request.accountType())
                .balance(request.initialBalance())
                .build());

        Customer refreshed = customerRepository.findById(customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        return CustomerResponse.from(refreshed);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
        return CustomerResponse.from(customer);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(CustomerResponse::from)
                .toList();
    }
}
