package com.codemich.quickpaybank.account;

import com.codemich.quickpaybank.account.dto.CreateAccountRequest;
import com.codemich.quickpaybank.account.dto.CreateCustomerRequest;
import com.codemich.quickpaybank.account.dto.CustomerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gerenciamento de clientes e suas contas")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Cadastrar cliente", description = "Cria um novo cliente no sistema")
    @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso")
    @ApiResponse(responseCode = "422", description = "CPF ou e-mail já cadastrado")
    public ResponseEntity<CustomerResponse> create(@RequestBody @Valid CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Criar conta", description = "Adiciona uma conta corrente ou poupança a um cliente existente")
    @ApiResponse(responseCode = "201", description = "Conta criada com sucesso")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    @ApiResponse(responseCode = "422", description = "Cliente já possui esse tipo de conta")
    public ResponseEntity<CustomerResponse> addAccount(@RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.addAccount(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente", description = "Retorna um cliente com suas contas pelo ID")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    public ResponseEntity<CustomerResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Listar clientes", description = "Retorna todos os clientes cadastrados com suas contas")
    @ApiResponse(responseCode = "200", description = "Lista de clientes")
    public ResponseEntity<List<CustomerResponse>> findAll() {
        return ResponseEntity.ok(customerService.findAll());
    }
}
