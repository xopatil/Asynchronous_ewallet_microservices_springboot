# 💳 Sterling E-Wallet — Microservices Architecture

A production-style **E-Wallet system** built with **Spring Boot Microservices**, featuring user management, wallet operations, and fund transfers with **asynchronous communication via RabbitMQ**, the **Outbox Pattern**, and **OAuth2-style JWT Authentication**.

---

## 📌 Project Overview

Sterling Corporation's E-Wallet allows users to:
- Register and authenticate securely with OAuth2-style Bearer Token
- Create and manage digital wallets
- Transfer funds between users
- Make merchant payments

The system is built on a **microservices architecture** where each service is independently deployable, scalable, and fault-tolerant. Inter-service communication uses both **synchronous (Feign Client)** and **asynchronous (RabbitMQ)** patterns. All requests are authenticated at the **API Gateway level** before reaching any microservice.

---

## 🏗️ Architecture

```
Client (Postman / App)
         │
         ▼
   API Gateway (:8080)          ← Single entry point
         │                         JWT validated HERE first
         │                         ↕ Eureka for service routing
         ├──────────────────────────────────────┐
         │                                      │
         ▼                                      ▼
  User Service (:8082)            Wallet Service (:8083)
  - Registration                  - Create Wallet
  - Login → OAuth2 Token          - Top Up
  - JWT Auth + BCrypt             - Deduct / Credit Balance
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
| **Spring Cloud Gateway** | API Gateway — single entry point + JWT enforcement |
| **Spring Cloud Netflix Eureka** | Service discovery and registration |
| **Spring Security + JJWT** | OAuth2-style Bearer Token authentication |
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
│
├── api-gateway/                      # Routes all requests + JWT enforcement
│   └── src/main/java/com/sterling/api_gateway/
│       └── security/
│           ├── JwtUtil.java          # Token validation at gateway level
│           └── JwtAuthFilter.java    # Global filter — blocks invalid tokens
│
├── user-service/                     # User registration, login, JWT issuer
│   └── src/main/java/com/sterling/user_service/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── model/
│       ├── dto/
│       │   ├── RegisterRequest.java
│       │   ├── LoginRequest.java
│       │   └── TokenResponse.java    # OAuth2-style token response
│       └── security/
│           ├── JwtUtil.java
│           ├── JwtFilter.java
│           ├── SecurityConfig.java
│           └── CustomUserDetailsService.java
│
├── wallet-service/                   # Wallet and balance management
│   └── src/main/java/com/sterling/wallet_service/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── model/
│       ├── dto/
│       ├── security/
│       │   └── JwtUtil.java          # Token validation
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

## 🔐 OAuth2-Style JWT Authentication

### How it Works

The project does not use an external OAuth2 Authorization Server (Keycloak, Auth0, AWS Cognito). Instead it implements **OAuth2-style Bearer Token authentication** using self-signed JWTs via the JJWT library:

```
1. User registers → password BCrypt hashed → stored in DB
2. User logs in → credentials verified → JWT token issued by User Service
3. Login response follows OAuth2 token format
4. Client sends token in every request: Authorization: Bearer <token>
5. API Gateway JwtAuthFilter validates token FIRST
6. Invalid token → 401 Unauthorized (never reaches service)
7. Valid token → X-Username header added → forwarded to service
8. Individual services also validate as a second security layer
```

### Login Response (OAuth2 Standard Format)

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "username": "alice"
}
```

### Public Endpoints — No Token Required

| Endpoint | Reason |
|---|---|
| `POST /users/register` | Users must register without a token |
| `POST /users/login` | Users must login to GET a token |
| `GET /actuator/**` | Health monitoring |

### All Other Endpoints — Require `Authorization: Bearer <token>`

---

## 🔄 Asynchronous Communication — Outbox Pattern with RabbitMQ

### The Problem with Pure Synchronous Communication

In the original architecture, Transaction Service called Wallet Service directly via Feign Client:
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
    (background — 5 seconds later)
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
# Terminal 1 — Eureka Server (always first)
cd eureka-server && mvn spring-boot:run

# Terminal 2 — User Service
cd user-service && mvn spring-boot:run

# Terminal 3 — Wallet Service
cd wallet-service && mvn spring-boot:run

# Terminal 4 — Transaction Service
cd transaction-service && mvn spring-boot:run

# Terminal 5 — API Gateway (always last)
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

### Authentication Header
```
Authorization: Bearer <access_token>
```

---

### User Service

| Method | Endpoint | Auth | Body | Description |
|---|---|---|---|---|
| POST | `/users/register` | None | `{username, email, password}` | Register new user |
| POST | `/users/login` | None | `{username, password}` | Login — returns OAuth2 token |
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

## 🧪 Complete Demo Flow

### Step 1 — Register Alice
```json
POST http://localhost:8080/users/register
Content-Type: application/json

{"username":"alice","email":"alice@gmail.com","password":"pass123"}
```
Expected: `201 Created`

### Step 2 — Register Bob
```json
POST http://localhost:8080/users/register
Content-Type: application/json

{"username":"bob","email":"bob@gmail.com","password":"pass123"}
```
Expected: `201 Created`

### Step 3 — Login (Get OAuth2 Token)
```json
POST http://localhost:8080/users/login
Content-Type: application/json

{"username":"alice","password":"pass123"}
```
Expected `200 OK`:
```json
{
  "access_token": "eyJhbGci...",
  "token_type": "Bearer",
  "expires_in": 86400,
  "username": "alice"
}
```
Copy `access_token` — use it in all requests below.

### Step 4 — Prove Gateway Blocks Unauthenticated Requests
```
GET http://localhost:8080/wallet/balance/1
(no Authorization header)
```
Expected: `401 Unauthorized` — proves Gateway JWT enforcement works.

### Step 5 — Create Wallets
```
POST http://localhost:8080/wallet/create?userId=1&username=alice
Authorization: Bearer <access_token>

POST http://localhost:8080/wallet/create?userId=2&username=bob
Authorization: Bearer <access_token>
```

### Step 6 — Top Up Alice
```json
POST http://localhost:8080/wallet/topup
Authorization: Bearer <access_token>
Content-Type: application/json

{"userId":1,"amount":1000.00}
```
Expected: balance = `1000.00`

### Step 7 — Check Balances Before Transfer
```
GET http://localhost:8080/wallet/balance/1    → 1000.00
GET http://localhost:8080/wallet/balance/2    → 0.00
```

### Step 8 — Async Transfer (Key Demo)
```json
POST http://localhost:8080/transactions/transfer
Authorization: Bearer <access_token>
Content-Type: application/json

{"senderUserId":1,"receiverUserId":2,"amount":200.00,"description":"Async demo"}
```
Response comes back **immediately**. Watch console logs for the 5-second async flow.

### Step 9 — Check Outbox Table (H2 Console)
```
URL:      http://localhost:8084/h2-console
JDBC URL: jdbc:h2:mem:transactiondb
Username: sa   Password: (blank)

SELECT * FROM outbox_messages;
-- Shows PENDING/SENT during processing, then 0 rows after ACK
```

### Step 10 — Verify Balances After Transfer
```
GET http://localhost:8080/wallet/balance/1    → 800.00
GET http://localhost:8080/wallet/balance/2    → 200.00
```

### Step 11 — Transaction History
```
GET http://localhost:8080/transactions/history/1
Authorization: Bearer <access_token>
```

### Step 12 — Merchant Payment
```json
POST http://localhost:8080/transactions/merchant-payment
Authorization: Bearer <access_token>
Content-Type: application/json

{"customerUserId":1,"merchantUserId":2,"amount":100.00,"description":"Merchant demo"}
```

---

## 🗄️ Database Reference

| Service | DB Name | H2 Console | JDBC URL |
|---|---|---|---|
| User Service | `userdb` | `http://localhost:8082/h2-console` | `jdbc:h2:mem:userdb` |
| Wallet Service | `walletdb` | `http://localhost:8083/h2-console` | `jdbc:h2:mem:walletdb` |
| Transaction Service | `transactiondb` | `http://localhost:8084/h2-console` | `jdbc:h2:mem:transactiondb` |

> H2 is in-memory. Data resets on restart. Replace with MySQL/PostgreSQL for production.

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

Log files written to each service's `logs/` folder. All use `@Slf4j`.

| Level | Used for |
|---|---|
| `INFO` | Business events — transfers, registrations, ACKs, token issuance |
| `DEBUG` | Technical details — SQL queries, JWT validation, routing |
| `WARN` | Non-critical — retry attempts, duplicate registrations |
| `ERROR` | Failures with full stack trace |

---

## 🔍 Health Monitoring

```
GET http://localhost:{port}/actuator/health
GET http://localhost:{port}/actuator/info
```

---

## 🧱 Design Patterns Used

| Pattern | Where Used |
|---|---|
| **OAuth2 Bearer Token** | API Gateway — centralized JWT enforcement |
| **Outbox Pattern** | Transaction Service — zero message loss guarantee |
| **Service Registry** | Eureka — dynamic service discovery |
| **API Gateway Pattern** | Single entry point — routing, auth, load balancing |
| **Repository Pattern** | All services — data access abstraction |
| **DTO Pattern** | All services — separates API contract from DB model |
| **Pessimistic Recording** | Transaction Service — saves FAILED first, updates to SUCCESS |

---

## 👨‍💻 Author

**Soham Patil**
Internship Project — Sterling Corporation E-Wallet System
Spring Boot Microservices · RabbitMQ Async · OAuth2-style JWT Authentication
