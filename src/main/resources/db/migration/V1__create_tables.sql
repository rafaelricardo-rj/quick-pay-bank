CREATE TABLE tb_customer (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    document   VARCHAR(14)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL,
    CONSTRAINT uk_customer_document UNIQUE (document),
    CONSTRAINT uk_customer_email    UNIQUE (email)
);

CREATE TABLE tb_account (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT                      NOT NULL,
    account_type ENUM('CHECKING', 'SAVINGS') NOT NULL,
    balance      DECIMAL(15, 2)              NOT NULL DEFAULT 0.00,
    created_at   DATETIME                    NOT NULL,
    updated_at   DATETIME,
    CONSTRAINT fk_account_customer      FOREIGN KEY (customer_id) REFERENCES tb_customer (id),
    CONSTRAINT uk_account_customer_type UNIQUE (customer_id, account_type)
);

CREATE TABLE tb_transfer (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_id   BIGINT                       NOT NULL,
    payee_id   BIGINT                       NOT NULL,
    amount     DECIMAL(15, 2)               NOT NULL,
    status     ENUM('COMPLETED', 'FAILED')  NOT NULL,
    created_at DATETIME                     NOT NULL,
    CONSTRAINT fk_transfer_payer FOREIGN KEY (payer_id) REFERENCES tb_account (id),
    CONSTRAINT fk_transfer_payee FOREIGN KEY (payee_id) REFERENCES tb_account (id)
);
