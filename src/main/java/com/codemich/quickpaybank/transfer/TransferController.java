package com.codemich.quickpaybank.transfer;

import com.codemich.quickpaybank.transfer.audit.TransferAuditService;
import com.codemich.quickpaybank.transfer.audit.dto.TransferAuditResponse;
import com.codemich.quickpaybank.transfer.dto.TransferRequest;
import com.codemich.quickpaybank.transfer.dto.TransferResponse;
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
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transferências", description = "Operações de transferência entre contas")
public class TransferController {

    private final TransferService transferService;
    private final TransferAuditService auditService;

    @PostMapping
    @Operation(
            summary = "Realizar transferência",
            description = "Transfere um valor de uma conta para outra. " +
                    "Contas poupança têm limite de R$ 2.000,00 por transferência e R$ 5.000,00 por dia."
    )
    @ApiResponse(responseCode = "201", description = "Transferência realizada com sucesso")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    @ApiResponse(responseCode = "422", description = "Regra de negócio violada (saldo insuficiente, limites de poupança)")
    public ResponseEntity<TransferResponse> transfer(@RequestBody @Valid TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transferService.transfer(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar transferência", description = "Retorna os detalhes de uma transferência pelo ID")
    @ApiResponse(responseCode = "200", description = "Transferência encontrada")
    @ApiResponse(responseCode = "404", description = "Transferência não encontrada")
    public ResponseEntity<TransferResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.findById(id));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
            summary = "Extrato da conta",
            description = "Retorna todas as transferências (enviadas e recebidas) de uma conta, ordenadas da mais recente"
    )
    @ApiResponse(responseCode = "200", description = "Extrato da conta")
    public ResponseEntity<List<TransferResponse>> findByAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(transferService.findByAccountId(accountId));
    }

    @GetMapping("/audit/account/{accountId}")
    @Operation(
            summary = "Auditoria da conta",
            description = "Retorna todas as tentativas de transferência (sucesso e falha) de uma conta"
    )
    @ApiResponse(responseCode = "200", description = "Histórico de auditoria")
    public ResponseEntity<List<TransferAuditResponse>> findAuditByAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(auditService.findByAccountId(accountId));
    }
}
