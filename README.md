# Quick Pay Bank API

API REST para simulação de operações de um banco digital, contemplando gerenciamento de contas, transferências entre clientes e registro de movimentações financeiras.

---

## Tecnologias

- **Java 17**
- **Spring Boot 3.5**
- **Spring Data JPA + Hibernate**
- **MySQL 8**
- **Flyway** — versionamento de banco de dados
- **springdoc-openapi** — documentação Swagger
- **Lombok**
- **JUnit 5 + Mockito** — testes unitários
- **Docker + Docker Compose**

---

## Como rodar

### Opção 1 — Docker Compose (recomendado)

Sobe a aplicação e o banco MySQL juntos, sem necessidade de configuração local.

**Pré-requisito:** Docker instalado.

```bash
docker compose up --build
```

A aplicação aguarda o MySQL estar pronto antes de iniciar. O Flyway executa as migrations automaticamente na primeira inicialização.

Para rodar em segundo plano:
```bash
docker compose up --build -d
```

Para derrubar os containers:
```bash
docker compose down
```

Para derrubar e resetar o banco (apaga o volume):
```bash
docker compose down -v
```

---

### Opção 2 — Localmente com Maven

**Pré-requisitos:** Java 17, Maven e MySQL rodando localmente.

1. Crie o banco e o usuário no MySQL:

```sql
CREATE DATABASE quickpay;
CREATE USER 'qpayuser'@'%' IDENTIFIED BY 'qpaypaswd';
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES, INSERT, UPDATE, DELETE, SELECT ON quickpay.* TO 'qpayuser'@'%';
FLUSH PRIVILEGES;
```

2. Suba a aplicação com o perfil `dev`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev` usa por padrão `127.0.0.1:3307`. Para sobrescrever:
```bash
DB_HOST=localhost DB_USER=qpayuser DB_PASSWD=qpaypaswd \
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### Rodando os testes

```bash
./mvnw test
```

---

### Endpoints disponíveis

| Recurso | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| API Docs (JSON) | http://localhost:8080/api-docs |

O arquivo `requests.http` na raiz do projeto contém todas as chamadas prontas para uso no IntelliJ HTTP Client.

---

## Dados pré-carregados (seed)

O Flyway popula automaticamente 3 clientes, cada um com conta corrente e poupança:

| ID | Cliente | Conta | Saldo |
|---|---|---|---|
| 1 | Ana Silva | Corrente | R$ 10.000,00 |
| 2 | Ana Silva | Poupança | R$ 5.000,00 |
| 3 | Bruno Costa | Corrente | R$ 8.000,00 |
| 4 | Bruno Costa | Poupança | R$ 3.000,00 |
| 5 | Carla Mendes | Corrente | R$ 15.000,00 |
| 6 | Carla Mendes | Poupança | R$ 7.500,00 |

---

## Arquitetura

### Organização por feature

O projeto é organizado por funcionalidade em vez de camada técnica, mantendo cada feature coesa e independente:

```
com.codemich.quickpayiasupport
├── account/           → clientes e contas (entidades, repositórios, serviços, controllers, DTOs)
├── transfer/
│   ├── audit/         → auditoria de tentativas de transferência
│   └── validation/    → Strategy Pattern para validações por tipo de conta
├── notification/      → envio assíncrono de notificações
└── shared/
    ├── config/        → configurações (Async, OpenAPI, RestClient)
    └── exception/     → exceções de domínio e handler global
```

---

## Decisões de design

### Modelo de dados — separação entre cliente e conta

Um `Customer` pode ter múltiplas contas, uma por tipo (`CHECKING`, `SAVINGS`). Isso evita duplicação de dados pessoais e reflete o modelo real de banco, onde o cliente é uma entidade e as contas são instrumentos financeiros associados a ele.

```
tb_customer ──< tb_account ──< tb_transfer
              UNIQUE(customer_id, account_type)
```

A tabela `tb_transfer_audit` não possui FK para `tb_account` intencionalmente — registros de auditoria são histórico imutável e não devem ser afetados por mudanças nas contas.

---

### Regras de negócio por tipo de conta

| Regra | Conta Corrente | Conta Poupança |
|---|---|---|
| Limite por transferência | sem limite | R$ 2.000,00 |
| Limite diário de saída | sem limite | R$ 5.000,00 |

---

### Strategy Pattern para validações

As regras de negócio específicas de cada tipo de conta são encapsuladas em classes separadas que implementam `TransferValidationStrategy`. O `TransferService` delega a validação para a strategy correspondente ao tipo da conta pagadora, sem `if/else`:

```java
validationStrategies.get(payer.getAccountType()).validate(payer, request.amount());
```

Para adicionar um novo tipo de conta basta criar uma nova classe `@Component` implementando a interface — o Spring a registra automaticamente no mapa de strategies sem nenhuma alteração no `TransferService` (**Open/Closed Principle**).

---

### Consistência transacional e controle de concorrência

Toda a operação de transferência é executada dentro de uma única `@Transactional`, garantindo atomicidade — em caso de falha, todos os passos são revertidos.

Para evitar condições de corrida em ambiente de alta concorrência, as contas são bloqueadas com `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`). Os locks são sempre adquiridos na **ordem crescente de ID**, independente da direção da transferência, o que elimina a possibilidade de deadlock entre transações concorrentes.

---

### Auditoria com transação separada

O `TransferAuditService` registra toda tentativa de transferência com dois comportamentos distintos:

- **`logSuccess`** — participa da transação principal. Se a transferência for revertida, o log de sucesso também é.
- **`logFailure`** — usa `REQUIRES_NEW`, abrindo uma transação independente que commita mesmo quando a transação principal sofre rollback.

Isso garante que falhas de negócio (saldo insuficiente, limites excedidos, conta inexistente) sejam sempre registradas na tabela `tb_transfer_audit`.

---

### Notificações assíncronas

Após uma transferência bem-sucedida, uma notificação é enviada ao recebedor via `POST` para um serviço externo configurável (`notification.url`). O envio é feito com `@Async` em um thread pool dedicado, de modo que uma eventual falha na notificação não impacta a resposta da transferência nem causa rollback.

---

### Versionamento de banco com Flyway

| Migration | Descrição |
|---|---|
| `V1__create_tables.sql` | DDL completo — `tb_customer`, `tb_account`, `tb_transfer` |
| `V2__seed_data.sql` | 3 clientes com conta corrente e poupança |
| `V3__remove_transfer_status_column.sql` | Remoção da coluna `status` de `tb_transfer` |
| `V4__create_transfer_audit_table.sql` | Tabela de auditoria `tb_transfer_audit` |
