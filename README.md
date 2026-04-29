# 💳 Sterling E-Wallet — Microservices Architecture

A production-style **E-Wallet system** built with **Spring Boot Microservices**, featuring user management, wallet operations, and fund transfers with **asynchronous communication via RabbitMQ** and the **Outbox Pattern**.


---

## 📌 Project Overview

Sterling Corporation's E-Wallet allows users to:
- Register and authenticate securely
- Create and manage digital wallets
- Transfer funds between users
- Make merchant payments

The system is built on a **microservices architecture** where each service is independently deployable, scalable, and fault-tolerant. Inter-service communication uses both **synchronous (Feign Client)** and **asynchronous (RabbitMQ)** patterns.

---

## 🏗️ Architecture

```
Client (Postman / App)
         │
         ▼
   API Gateway (:8080)          ← Single entry point, routes all requests
         │
         ├──────────────────────────────────────┐
         │                                      │
         ▼                                      ▼
  User Service (:8082)            Wallet Service (:8083)
  - Registration                  - Create Wallet
  - Login + JWT                   - Top Up
  - Authentication                - Deduct / Credit Balance
         │                                      ▲
         │                                      │ RabbitMQ (Async)
         ▼                                      │
Transaction Service (:8084) ────────────────────┘
  - Fund Transfers                ← Publishes to RabbitMQ queue
  - Merchant Payments             ← Outbox Pattern for reliability
  - Transaction History

         All services register with →  Eureka Server (:8761)
```

---

## ⚙️ Tech Stack

| Technology | Purpose |
|---|---|
| **Spring Boot 3.2.x** | Core framework for all microservices |
| **Spring Cloud Gateway** | API Gateway — single entry point |
| **Spring Cloud Netflix Eureka** | Service discovery and registration |
| **Spring Security + JWT** | Authentication and authorization |
| **Spring Data JPA + H2** | Database layer (in-memory for development) |
| **Spring AMQP + RabbitMQ** | Asynchronous messaging between services |
| **OpenFeign** | Synchronous service-to-service HTTP calls |
| **Lombok** | Boilerplate code reduction |
| **Spring Boot Actuator** | Health monitoring endpoints |
| **SLF4J + Logback** | Structured logging across all services |
| **Java 17** | Language version |
| **Maven** | Build and dependency management |

---

## 📁 Project Structure

```
sterling-ewallet/
│
├── eureka-server/                    # Service registry
├── api-gateway/                      # Routes all incoming requests
├── user-service/                     # User registration, login, JWT
│   └── src/main/java/com/sterling/user_service/
│       ├── controller/               # REST endpoints
│       ├── service/                  # Business logic
│       ├── repository/               # Database operations
│       ├── model/                    # User entity
│       ├── dto/                      # Request/Response objects
│       └── security/                 # JWT + Spring Security
│
├── wallet-service/                   # Wallet and balance management
│   └── src/main/java/com/sterling/wallet_service/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── model/
│       ├── dto/
│       ├── config/                   # RabbitMQ configuration
│       └── messaging/                # RabbitMQ listener + ACK publisher
│
└── transaction-service/              # Transfers and payments
    └── src/main/java/com/sterling/transaction_service/
        ├── controller/
        ├── service/
        ├── repository/
        ├── model/                    # Transaction + OutboxMessage entities
        ├── dto/
        ├── client/                   # Feign clients
        ├── config/                   # RabbitMQ configuration
        ├── messaging/                # Publisher + ACK listener
        └── scheduler/                # BacklogProcessor (@Scheduled)
```

---

## 🔄 Asynchronous Communication — Outbox Pattern with RabbitMQ

### The Problem with Pure Synchronous Communication
In the original architecture, Transaction Service called Wallet Service directly via Feign Client. This meant:
- Client had to wait for BOTH services to complete
- If Wallet Service was down → transfer failed completely
- Services were tightly coupled

### The Solution — Outbox Pattern + RabbitMQ

```
POST /transactions/transfer
         │
         ▼
Transaction Service validates request
         │
         ▼
Feign: deduct + credit wallet (instant check)
         │
         ▼
Save transaction to DB (status = SUCCESS)
         │
         ▼
Write to OUTBOX TABLE (status = PENDING)
         │
         ▼
Return response to Client ✅  ← Client done. Doesn't wait anymore.
         │
    (background)
         │
         ▼
BacklogProcessor (@Scheduled every 5s)
reads PENDING rows → publishes to RabbitMQ
         │
         ▼
wallet.update.queue
         │
         ▼
WalletUpdateListener (Wallet Service)
processes update → sends ACK
         │
         ▼
ack.queue
         │
         ▼
AckListener (Transaction Service)
deletes outbox row ✅ — fully acknowledged
```

### Key Components

| Component | Service | Role |
|---|---|---|
| `OutboxMessage.java` | Transaction | DB table storing pending messages |
| `BacklogProcessor.java` | Transaction | @Scheduled — reads PENDING, publishes to RabbitMQ |
| `WalletUpdatePublisher.java` | Transaction | Sends messages to RabbitMQ |
| `AckListener.java` | Transaction | Receives ACK, deletes outbox row |
| `WalletUpdateListener.java` | Wallet | @RabbitListener — processes wallet update |
| `AckPublisher.java` | Wallet | Sends ACK after successful processing |
| `RabbitMQConfig.java` | Both | Declares queues, exchange, bindings |

### RabbitMQ Setup

| Component | Name |
|---|---|
| Exchange | `sterling.exchange` (DirectExchange) |
| Transfer Queue | `wallet.update.queue` |
| ACK Queue | `ack.queue` |
| Transfer Routing Key | `wallet.update` |
| ACK Routing Key | `ack.response` |

---

## 🔐 Security

- All endpoints except `/users/register` and `/users/login` require a **JWT Bearer Token**
- Passwords are hashed using **BCrypt** — never stored as plain text
- JWT tokens expire after **24 hours**
- Spring Security configured as **stateless** (no server-side sessions)

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |
| RabbitMQ | 3.12+ |
| Erlang | 25+ or 26+ |

### 1. Start RabbitMQ

**Windows:**
```bash
# Navigate to RabbitMQ sbin folder
cd "C:\Program Files\RabbitMQ Server\rabbitmq_server-x.x.x\sbin"
rabbitmq-server.bat start
```

**Mac:**
```bash
brew services start rabbitmq
```

Verify at: `http://localhost:15672` (guest / guest)

### 2. Start Services in Order

```bash
# Terminal 1 — Eureka Server
cd eureka-server && mvn spring-boot:run

# Terminal 2 — User Service
cd user-service && mvn spring-boot:run

# Terminal 3 — Wallet Service
cd wallet-service && mvn spring-boot:run

# Terminal 4 — Transaction Service
cd transaction-service && mvn spring-boot:run

# Terminal 5 — API Gateway (start last)
cd api-gateway && mvn spring-boot:run
```

### 3. Verify Everything is Running

| URL | Expected |
|---|---|
| `http://localhost:8761` | Eureka dashboard — all 4 services listed |
| `http://localhost:15672` | RabbitMQ dashboard — 2 queues visible |
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` |

---

## 📡 API Reference

### Base URL
```
http://localhost:8080
```

### User Service

| Method | Endpoint | Auth | Body | Description |
|---|---|---|---|---|
| POST | `/users/register` | None | `{username, email, password}` | Register new user |
| POST | `/users/login` | None | `{username, password}` | Login — returns JWT token |
| GET | `/users/{username}` | JWT | — | Get user details |

### Wallet Service

| Method | Endpoint | Auth | Body | Description |
|---|---|---|---|---|
| POST | `/wallet/create` | JWT | `?userId=1&username=alice` | Create wallet for user |
| POST | `/wallet/topup` | JWT | `{userId, amount}` | Add funds to wallet |
| GET | `/wallet/balance/{userId}` | JWT | — | Get wallet balance |

### Transaction Service

| Method | Endpoint | Auth | Body | Description |
|---|---|---|---|---|
| POST | `/transactions/transfer` | JWT | `{senderUserId, receiverUserId, amount, description}` | Transfer funds |
| POST | `/transactions/merchant-payment` | JWT | `{customerUserId, merchantUserId, amount, description}` | Merchant payment |
| GET | `/transactions/history/{userId}` | JWT | — | Get transaction history |
| GET | `/transactions/{id}` | JWT | — | Get transaction by ID |

---

## 🧪 Testing the Async Flow

### Step 1 — Register Users
```json
POST /users/register
{ "username": "alice", "email": "alice@gmail.com", "password": "pass123" }

POST /users/register
{ "username": "bob", "email": "bob@gmail.com", "password": "pass123" }
```

### Step 2 — Login
```json
POST /users/login
{ "username": "alice", "password": "pass123" }
```
Copy the JWT token from the response.

### Step 3 — Setup Wallets
```json
POST /wallet/create?userId=1&username=alice
POST /wallet/create?userId=2&username=bob

POST /wallet/topup
{ "userId": 1, "amount": 1000.00 }
```

### Step 4 — Transfer and Watch Async in Action
```json
POST /transactions/transfer
{
  "senderUserId": 1,
  "receiverUserId": 2,
  "amount": 200.00,
  "description": "Async transfer demo"
}
```

**What to observe in logs:**
```
[10:00:00] Transaction Service → Transfer SUCCESS. TransactionId: 1
[10:00:00] Transaction Service → Outbox row written. Status: PENDING
[10:00:00] Postman receives 200 OK ← Client done

(5 seconds later — background processing)

[10:00:05] Transaction Service → BacklogProcessor found 1 PENDING message
[10:00:05] Transaction Service → Message published to RabbitMQ
[10:00:05] Wallet Service      → RabbitMQ message received. TransactionId: 1
[10:00:05] Wallet Service      → Wallet update SUCCESS
[10:00:05] Wallet Service      → ACK published
[10:00:05] Transaction Service → ACK received → Outbox row DELETED
```

The **5 second gap** between client response and wallet update is proof of asynchronous communication.

### Verify via H2 Console

```
URL:      http://localhost:8084/h2-console
JDBC URL: jdbc:h2:mem:transactiondb
Username: sa
Password: (blank)
```

```sql
-- Shows PENDING/SENT rows during processing
SELECT * FROM outbox_messages;

-- Should be empty after ACK received
SELECT COUNT(*) FROM outbox_messages;
```

---

## 🗄️ Database Setup

Each service has its own **isolated H2 in-memory database**:

| Service | DB Name | Console URL |
|---|---|---|
| User Service | `userdb` | `http://localhost:8082/h2-console` |
| Wallet Service | `walletdb` | `http://localhost:8083/h2-console` |
| Transaction Service | `transactiondb` | `http://localhost:8084/h2-console` |

> **Note:** H2 is an in-memory database. All data resets when services restart. For production, replace with MySQL or PostgreSQL.

---

## 📊 Port Reference

| Service | Port |
|---|---|
| Eureka Server | 8761 |
| API Gateway | 8080 |
| User Service | 8082 |
| Wallet Service | 8083 |
| Transaction Service | 8084 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Dashboard | 15672 |

---

## 📝 Logging

All services use **SLF4J with Logback** via Lombok's `@Slf4j` annotation. Log files are written to:

```
user-service/logs/user-service.log
wallet-service/logs/wallet-service.log
transaction-service/logs/transaction-service.log
```

Log levels:
- `INFO` — business events (transfers, registrations, ACKs)
- `DEBUG` — technical details (SQL, method calls)
- `WARN` — non-critical issues (retry attempts)
- `ERROR` — failures with full stack trace

---

## 🔍 Health Monitoring

All services expose Spring Boot Actuator endpoints:

```
GET http://localhost:{port}/actuator/health
GET http://localhost:{port}/actuator/info
```

---

## 🧱 Design Patterns Used

| Pattern | Where Used |
|---|---|
| **Outbox Pattern** | Transaction Service — guarantees zero message loss |
| **Service Registry** | Eureka — dynamic service discovery |
| **API Gateway** | Single entry point — routing and load balancing |
| **Repository Pattern** | All services — data access abstraction |
| **DTO Pattern** | All services — separates API contract from DB model |
| **Pessimistic Recording** | Transaction Service — saves FAILED first, updates to SUCCESS |

---

## 👨‍💻 Author

**Soham Patil**
Internship Project — Sterling Corporation E-Wallet System
Built with Spring Boot Microservices + RabbitMQ Async Architecture
