INSERT INTO tb_customer (name, document, email, created_at) VALUES
    ('Ana Silva',    '11111111111', 'ana.silva@email.com',    NOW()),
    ('Bruno Costa',  '22222222222', 'bruno.costa@email.com',  NOW()),
    ('Carla Mendes', '33333333333', 'carla.mendes@email.com', NOW());

INSERT INTO tb_account (customer_id, account_type, balance, created_at, updated_at) VALUES
    (1, 'CHECKING', 10000.00, NOW(), NOW()),
    (1, 'SAVINGS',   5000.00, NOW(), NOW()),
    (2, 'CHECKING',  8000.00, NOW(), NOW()),
    (2, 'SAVINGS',   3000.00, NOW(), NOW()),
    (3, 'CHECKING', 15000.00, NOW(), NOW()),
    (3, 'SAVINGS',   7500.00, NOW(), NOW());
