CREATE TABLE tb_transfer_audit (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_id   BIGINT                       NOT NULL,
    payee_id   BIGINT                       NOT NULL,
    amount     DECIMAL(15, 2)               NOT NULL,
    status     ENUM('SUCCESS', 'FAILED')    NOT NULL,
    reason     VARCHAR(500),
    created_at DATETIME                     NOT NULL
);
