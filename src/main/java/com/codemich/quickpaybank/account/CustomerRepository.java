package com.codemich.quickpaybank.account;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);
}
