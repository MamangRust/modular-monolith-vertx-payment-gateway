# Distributed Modular Monolith — Payment Gateway Platform (Eclipse Vert.x)

A production-grade, highly resilient, and fully observable **modular-monolith payment gateway backend** built in **Java 21** using **Eclipse Vert.x** reactive toolkit (v4.5.24). Designed around domain-driven service boundaries following Clean Architecture and CQRS principles, it retains the operational and deployment simplicity of a single deployment unit while maintaining logical isolation typical of microservices.

Each financial and identity business domain — Users, Roles, Cards, Merchants, Saldo, Topups, Transactions, Transfers, Withdrawals — lives in its own self-contained Maven module. These modules communicate synchronously via high-performance **gRPC** protocols and asynchronously using **Apache Kafka** event propagation, exposing a unified reactive entry point through a **REST API Gateway** powered by Vert.x Web Router.

The platform is fortified with a **comprehensive observability suite** (Prometheus, Grafana, Loki, Jaeger, OpenTelemetry), robust connection pooling via **PgBouncer**, **distributed Redis Cluster caching** with custom telemetry for each service, and Kubernetes configurations ready for production auto-scaling.

---

## Key Features

| Domain | Capabilities |
| :--- | :--- |
| **Auth & Users** | Secure registration, multi-factor login, stateless JWT access/refresh token lifecycle, password reset workflows, OTP email verification, and `/me` profile REST endpoint. |
| **Roles & RBAC** | Custom permission configuration, granular access control matrices, and sub-second permission evaluation cached via Redis. |
| **Cards & VCC** | Virtual and debit card CRUD operations with soft-delete capabilities, card activation/suspension toggles, and multi-dimensional transaction analytics (daily/monthly/yearly topup, withdraw, transfer). |
| **Merchants** | Fully featured merchant onboarding, profile details management, business data registration, and merchant performance/transaction reports with full data restoration capabilities (soft delete & restore). |
| **Saldo (Balance)** | High-throughput, thread-safe real-time balance calculations, optimistic concurrency locks, and localized balances. |
| **Topup** | Balance loading ledger engine supporting multiple payment methods, detailed transactions logging, and soft-delete audit records. |
| **Transaction** | Centralized financial audit ledger collecting transaction events across the system, global search filters, status tracking, and monthly/yearly volume reports. |
| **Transfer** | Safe peer-to-peer card-to-card or user-to-user funds settlement with balance debit/credit synchronization and event-driven logging. |
| **Withdraw** | Funds settlement from user cards to external accounts/banks, daily transaction threshold limits, and status processing pipelines. |
| **Email Worker** | Kafka-driven asynchronous worker dispatching critical notification emails (OTPs, login alerts, merchant onboarding notices, and transfer/topup invoices) via SMTP. |
| **Observability** | Multi-dimensional metrics (Prometheus + Grafana), log aggregation (Loki + Logback), end-to-end distributed tracing (Jaeger + OpenTelemetry), and resource monitors (Node, Kafka, Postgres Exporters). |
| **Chaos Engine** | Built-in reactive chaos injection framework supporting dynamic YAML-based policies for HTTP routing, database/SQL interceptors, and resource stress constraints (CPU/Memory). |
| **Deployment** | Local orchestration using Docker Compose (featuring a 6-node Redis Cluster and PgBouncer), and auto-scaling Kubernetes manifests configured with Horizontal Pod Autoscalers (HPA). |

---

## Architecture Overview

The platform implements a **Distributed Modular Monolith** architecture. Each business service is logical, decoupled, and self-contained inside its own Maven submodule, possessing its own independent gRPC boundary. A **Vert.x REST API Gateway** acts as the unified edge router, transforming client HTTP REST requests into fast gRPC downstream communications via Vert.x gRPC clients.

### Core Architecture Principles

- **Domain-Driven Boundary Isolation**: Every service owns its database access, caching layers, and service logic, strictly forbidding cross-boundary database sharing.
- **Clean Architecture & CQRS**: Separation of concerns using `Handler (gRPC) → Service (Command/Query) → Repository (Command/Query)` layers ensures business logic remains clean, performant, and framework-agnostic.
- **Reactive Execution**: Powered entirely by the Vert.x event-loop execution model, utilizing non-blocking asynchronous APIs with `io.vertx.core.Future` for extreme throughput with minimal memory footprint.
- **PgBouncer Pooling**: Employs connection pooling to avoid PostgreSQL socket exhaustion across the multiple concurrent modular services.
- **Event-Driven Resilience**: Apache Kafka decouples transaction events, ensuring side effects like email billing remain completely non-blocking.
- **OTel Telemetry Integration**: Standardized OpenTelemetry middleware injects trace IDs across gRPC boundaries, allowing seamless trace propagation from the client REST gateway down to postgres operations.

```mermaid
graph TB
    classDef client fill:#0f172a,stroke:#38bdf8,color:#e0f2fe,stroke-width:2px,font-weight:bold
    classDef gateway fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,font-weight:bold
    classDef domain fill:#1e1b4b,stroke:#818cf8,color:#e0e7ff,stroke-width:1.5px
    classDef infra fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef obs fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef event fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:1.5px

    Client["Client Applications<br/>(Web / Mobile / API)"]:::client

    subgraph APIGateway["API Gateway — NGINX + Vert.x REST Gateway"]
        direction LR
        REST["REST API Route Handler<br/>Port :5000"]:::gateway
        AuthMW["JWT Auth & Role<br/>Middleware"]:::gateway
    end

    Client -->|HTTP REST| APIGateway

    subgraph BusinessServices["Business Domain Services (Eclipse Vert.x)"]
        direction TB

        subgraph IdentityDomain["Identity & Access"]
            AUTH["Auth Service<br/>JWT & BCrypt Server"]:::domain
            USER["User Service<br/>Profile Management"]:::domain
            ROLE["Role Service<br/>RBAC & Permissions"]:::domain
        end

        subgraph MerchantDomain["Merchant Management"]
            MERCH["Merchant Service<br/>Onboarding & Profiling"]:::domain
        end

        subgraph FinanceDomain["Finance & Ledger Suite"]
            CARD["Card Service<br/>VCC & Card Analytics"]:::domain
            SALDO["Saldo Service<br/>Real-time Balance Tracker"]:::domain
        end

        subgraph TransactionDomain["Transfers & Transactions"]
            TOPUP["Topup Service<br/>Balance Funding Engine"]:::domain
            TXN["Transaction Service<br/>Central Audit Register"]:::domain
            TRANSFER["Transfer Service<br/>P2P Card-to-Card Transfer"]:::domain
            WITHDRAW["Withdraw Service<br/>Outbound Fund Settlement"]:::domain
        end
    end

    REST -->|"Vert.x gRPC Client"| AUTH
    REST -->|"Vert.x gRPC Client"| USER
    REST -->|"Vert.x gRPC Client"| ROLE
    REST -->|"Vert.x gRPC Client"| MERCH
    REST -->|"Vert.x gRPC Client"| CARD
    REST -->|"Vert.x gRPC Client"| SALDO
    REST -->|"Vert.x gRPC Client"| TOPUP
    REST -->|"Vert.x gRPC Client"| TXN
    REST -->|"Vert.x gRPC Client"| TRANSFER
    REST -->|"Vert.x gRPC Client"| WITHDRAW

    subgraph Infrastructure["Infrastructure Layer"]
        direction LR
        PGBOUNCER["PgBouncer<br/>Connection Pooler :6432"]:::infra
        PG[("PostgreSQL<br/>PAYMENT_GATEWAY DB")]:::infra
        REDIS[("Redis Cluster<br/>6-Node Distributed Cache")]:::infra
        KAFKA[("Kafka Broker<br/>Event Bus")]:::infra
    end

    AUTH -->|"Reactive SQL Client"| PGBOUNCER
    USER -->|"Reactive SQL Client"| PGBOUNCER
    ROLE -->|"Reactive SQL Client"| PGBOUNCER
    MERCH -->|"Reactive SQL Client"| PGBOUNCER
    CARD -->|"Reactive SQL Client"| PGBOUNCER
    SALDO -->|"Reactive SQL Client"| PGBOUNCER
    TOPUP -->|"Reactive SQL Client"| PGBOUNCER
    TXN -->|"Reactive SQL Client"| PGBOUNCER
    TRANSFER -->|"Reactive SQL Client"| PGBOUNCER
    WITHDRAW -->|"Reactive SQL Client"| PGBOUNCER

    PGBOUNCER --> PG

    AUTH -->|"Vert.x Redis Client"| REDIS
    USER -->|"Vert.x Redis Client"| REDIS
    ROLE -->|"Vert.x Redis Client"| REDIS
    MERCH -->|"Vert.x Redis Client"| REDIS
    CARD -->|"Vert.x Redis Client"| REDIS
    SALDO -->|"Vert.x Redis Client"| REDIS
    REST -->|"Vert.x Redis Client"| REDIS

    subgraph EventConsumers["Event-Driven Consumers"]
        EMAIL["Email Service<br/>SMTP Notification Worker"]:::event
    end

    KAFKA -->|"Consume Events"| EMAIL

    subgraph Observability["Observability Stack"]
        direction LR
        PROM["Prometheus<br/>Metrics Engine"]:::obs
        LOKI["Loki<br/>Log Aggregator"]:::obs
        JAEGER["Jaeger<br/>Distributed Traces"]:::obs
        GRAFANA["Grafana<br/>Unified Dashboards"]:::obs
        OTEL["OTel Collector<br/>Telemetry Pipeline"]:::obs
        PROMTAIL["Promtail<br/>Log Shipper"]:::obs
        NODEX["Node Exporter<br/>System Metrics"]:::obs
        KAFKAX["Kafka Exporter<br/>Broker Metrics"]:::obs
        PGX["Postgres Exporter<br/>DB Performance"]:::obs
    end

    AUTH -->|gRPC| USER
    AUTH -->|gRPC| ROLE
    MERCH -->|gRPC| USER
    CARD -->|gRPC| USER
    TOPUP -->|gRPC| SALDO
    TOPUP -->|gRPC| TXN
    TRANSFER -->|gRPC| CARD
    TRANSFER -->|gRPC| SALDO
    TRANSFER -->|gRPC| TXN
    WITHDRAW -->|gRPC| CARD
    WITHDRAW -->|gRPC| SALDO
    WITHDRAW -->|gRPC| TXN

    AUTH -.->|"Publish Verification Event"| KAFKA
    TOPUP -.->|"Publish Topup Event"| KAFKA
    TRANSFER -.->|"Publish Transfer Event"| KAFKA
    WITHDRAW -.->|"Publish Withdraw Event"| KAFKA

    AUTH -.->|"/metrics"| PROM
    USER -.->|"/metrics"| PROM
    ROLE -.->|"/metrics"| PROM
    MERCH -.->|"/metrics"| PROM
    CARD -.->|"/metrics"| PROM
    SALDO -.->|"/metrics"| PROM
    TOPUP -.->|"/metrics"| PROM
    TXN -.->|"/metrics"| PROM
    TRANSFER -.->|"/metrics"| PROM
    WITHDRAW -.->|"/metrics"| PROM
    REST -.->|"/metrics"| PROM

    AUTH -.->|"OTLP Spans"| OTEL
    USER -.->|"OTLP Spans"| OTEL
    ROLE -.->|"OTLP Spans"| OTEL
    MERCH -.->|"OTLP Spans"| OTEL
    CARD -.->|"OTLP Spans"| OTEL
    SALDO -.->|"OTLP Spans"| OTEL
    TOPUP -.->|"OTLP Spans"| OTEL
    TXN -.->|"OTLP Spans"| OTEL
    TRANSFER -.->|"OTLP Spans"| OTEL
    WITHDRAW -.->|"OTLP Spans"| OTEL
    REST -.->|"OTLP Spans"| OTEL

    OTEL -.-> JAEGER
    PROMTAIL -.-> LOKI
    NODEX -.-> PROM
    KAFKAX -.-> PROM
    PGX -.-> PROM
    PROM -.-> GRAFANA
    LOKI -.-> GRAFANA
    JAEGER -.-> GRAFANA
    KAFKA -.-> KAFKAX
    PG -.-> PGX
```

---

## Service Catalog

The modular architecture consists of **12 logical micro-applications** plus supporting database and migrations:

```mermaid
graph LR
    classDef svc fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1px,rx:8
    classDef gw fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,rx:8,font-weight:bold
    classDef support fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1px,rx:8

    subgraph Gateway
        API["API Gateway<br/>Vert.x REST Router"]:::gw
    end

    subgraph Identity["Identity & Access (3)"]
        A1["auth"]:::svc
        A2["user"]:::svc
        A3["role"]:::svc
    end

    subgraph Merchant["Merchant Suite (1)"]
        M1["merchant"]:::svc
    end

    subgraph Finance["Finance & Card Suite (2)"]
        F1["card"]:::svc
        F2["saldo"]:::svc
    end

    subgraph Movements["Fund Transactions (4)"]
        T1["topup"]:::svc
        T2["transaction"]:::svc
        T3["transfer"]:::svc
        T4["withdraw"]:::svc
    end

    subgraph Support["Support Services (2)"]
        S1["email"]:::support
        S2["common"]:::support
    end

    API -->|"gRPC Client"| Identity
    API -->|"gRPC Client"| Merchant
    API -->|"gRPC Client"| Finance
    API -->|"gRPC Client"| Movements
```

---

## Internal Service Architecture

Every logical business service is mapped as a decoupled submodule following structured clean architecture rules.

```mermaid
graph TB
    classDef handler fill:#1e3a5f,stroke:#7dd3fc,color:#e0f2fe,stroke-width:1.5px
    classDef service fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1.5px
    classDef repo fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef infra fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef shared fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:1.5px

    subgraph Service["Maven Module: <service-name>/"]
        direction TB

        subgraph SrcJava["src/main/java/io/example/<service>/"]
            direction TB
            HANDLER["handler/<br/>gRPC Service Handlers"]:::handler
            SVC["service/ & service.impl/<br/>CQRS Business Logic"]:::service
            REPO["repository/<br/>Reactive Repositories"]:::repo
            MODEL["entity/ / domain/<br/>Entities & Domain Models"]:::repo
        end

        HANDLER --> SVC
        SVC --> REPO
        REPO --> MODEL
    end

    subgraph SharedLibs["common/ — Shared Maven Module"]
        direction LR
        CONFIG["config/<br/>AppConfig / JwtConfig"]:::shared
        FLYWAY["config/FlywayConfig<br/>Migrations Runner"]:::shared
        REDIS_CFG["config/RedisConfig<br/>Client Pools"]:::shared
        REDIS_SVC["service/RedisService<br/>Cache Actions"]:::shared
        OBS["observability/<br/>TracingMetrics / TelemetryConfig"]:::shared
        PB["proto stubs / pb<br/>gRPC Proto Stubs"]:::shared
    end

    subgraph Infrastructure["External Infrastructure"]
        direction LR
        PGDB[("PostgreSQL")]:::infra
        RCLUSTER[("Redis Cluster")]:::infra
        KAFKA[("Kafka Brokers")]:::infra
    end

    HANDLER --> PB
    SVC --> REDIS_SVC
    SVC --> OBS
    REPO --> PGDB
    REDIS_SVC --> RCLUSTER
```

---

## Data & Event Flow

### Synchronous Flow (REST Proxy & Cache Read-Through)

All external client API requests go through the REST endpoints defined in the Vert.x API Gateway Router. The API Gateway validates the JWT/API Key, connects with the correct downstream gRPC modular server, checks the Redis Cluster cache, and fetches PostgreSQL through PgBouncer if a cache miss occurs.

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant GW as API Gateway<br/>(Vert.x REST Router)
    participant SVC as Domain Service<br/>(gRPC Server)
    participant REDIS as Redis Cluster
    participant PGB as PgBouncer
    participant DB as PostgreSQL

    C->>GW: HTTP REST Request (GET/POST/PUT)
    GW->>GW: JWT Authentication Check
    GW->>SVC: gRPC Call (Protobuf payload)
    SVC->>REDIS: Check Cache (Redis Cluster)
    alt Cache Hit
        REDIS-->>SVC: Return Cached Response
    else Cache Miss
        SVC->>PGB: Acquire Connection
        PGB->>DB: Reactive SQL Execution
        DB-->>PGB: DB Result Set
        PGB-->>SVC: Reactive Rows Mapped
        SVC->>REDIS: Populate Cache for next read
    end
    SVC-->>GW: gRPC Response payload
    GW-->>C: HTTP REST Response (JSON format)
```

### Asynchronous Flow (Kafka Notification Event pipeline)

High-performance transaction modifications (like transfers or top-ups) trigger background notification events published directly to Apache Kafka brokers. The isolated Email service listens to Kafka, maps the events, and contacts SMTP services.

```mermaid
sequenceDiagram
    autonumber
    participant SVC as Transaction / Topup / Transfer
    participant K as Kafka Broker
    participant EMAIL as Email Worker Service
    participant SMTP as SMTP Server

    SVC->>K: Publish Event (e.g. transfer.created / topup.success)
    K-->>EMAIL: Deliver topic payload (asynchronous consumer)
    EMAIL->>EMAIL: Map payload details
    EMAIL->>SMTP: Send custom styled notification
    SMTP-->>EMAIL: Delivery Confirmation
```

---

## Observability Architecture

```mermaid
graph TB
    classDef service fill:#1e1b4b,stroke:#818cf8,color:#e0e7ff,stroke-width:1.5px
    classDef collector fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef storage fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef viz fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:2px,font-weight:bold

    subgraph Sources["Telemetry Sources"]
        direction TB
        SVCS["All Business Services<br/>(11 services)"]:::service
        KAFKA_SRC["Kafka Broker"]:::service
        NODES["Host / Node"]:::service
        DB_SRC["PostgreSQL Engine"]:::service
    end

    subgraph Collectors["Collection Layer"]
        direction TB
        PROM["Prometheus<br/>Scrapes /metrics"]:::collector
        PROMTAIL["Promtail<br/>Ships container logs"]:::collector
        OTEL["OTel Collector<br/>Receives OTLP spans"]:::collector
        NODEX["Node Exporter<br/>CPU / Memory / Disk / Net"]:::collector
        KAFKAX["Kafka Exporter<br/>Topic lag / Broker health"]:::collector
        PGX["Postgres Exporter<br/>PgBouncer & Query performance"]:::collector
    end

    subgraph Storage["Storage Layer"]
        direction TB
        PROM_TSDB["Prometheus TSDB<br/>(Metrics)"]:::storage
        LOKI_STORE["Loki<br/>(Log Index + Chunks)"]:::storage
        JAEGER_STORE["Jaeger<br/>(Trace Storage)"]:::storage
    end

    subgraph Visualization["Visualization & Alerting"]
        GRAFANA["Grafana<br/>Unified Dashboards"]:::viz
        ALERTMGR["Alertmanager<br/>Alert Routing"]:::viz
    end

    SVCS -->|"/metrics"| PROM
    SVCS -->|"OTLP gRPC"| OTEL
    SVCS -->|"stdout/stderr"| PROMTAIL
    NODES --> NODEX
    KAFKA_SRC --> KAFKAX
    DB_SRC --> PGX

    NODEX --> PROM
    KAFKAX --> PROM
    PGX --> PROM
    PROM --> PROM_TSDB
    PROMTAIL --> LOKI_STORE
    OTEL --> JAEGER_STORE

    PROM_TSDB --> GRAFANA
    LOKI_STORE --> GRAFANA
    JAEGER_STORE --> GRAFANA
    PROM_TSDB --> ALERTMGR
```

| Pillar | Tool | Purpose |
| :--- | :--- | :--- |
| **Metrics** | Prometheus + Grafana | Core metrics tracking (CPU, memory, request error rates, gRPC latencies, DB connection states). |
| **Logging** | Loki + Logback | Centralized structured JSON logger for indexing logs by service, queryable via LogQL. |
| **Tracing** | OpenTelemetry + Jaeger | Distributed system tracing across API gateway and internal gRPC services. |
| **Alerting** | Alertmanager | Automated notification system triggered during latency hikes or service disconnects. |


## Chaos Engineering Platform

The payment gateway features a built-in **reactive Chaos Engineering engine** to continuously test system resilience under failure conditions (database spikes, slow endpoints, CPU stress, and memory leaks). 

### How It Works
The chaos engine is managed by [ChaosManager.java](./common/src/main/java/io/example/common/chaos/ChaosManager.java) which dynamically watches the configuration file [chaos.yaml](./chaos.yaml) for modifications:
- **Dynamic Hot-Reloading**: Every 5 seconds, the engine checks `chaos.yaml` for changes. Adjusting values or toggling policies will update the running system instantly without requiring a service restart.

### Injection Mechanisms
1. **HTTP Routing Chaos** ([ChaosHttpMiddleware.java](./common/src/main/java/io/example/common/chaos/ChaosHttpMiddleware.java)): Intercepts API router entry points to inject specified latency hikes or HTTP errors (e.g., status code 429 - rate limits).
2. **Database SQL Chaos** ([ChaosSqlProxy.java](./common/src/main/java/io/example/common/chaos/ChaosSqlProxy.java)): Wraps database clients in a dynamic proxy, injecting database transaction latency or simulating sudden lock wait timeouts/deadlocks when queries hit matching tables.
3. **Resource Stress Chaos** ([ChaosResourceSabotage.java](.//common/src/main/java/io/example/common/chaos/ChaosResourceSabotage.java)): Spawns CPU/memory pressure routines to simulate container hardware throttling or memory exhaustion.

### Configuration Example (`chaos.yaml`)
To simulate failures, configure policies in [chaos.yaml](./chaos.yaml):
```yaml
policies:
  - name: "http-get-saldos-limit"
    type: "http"
    target: "GET:/api/saldos"
    enabled: true        # Enable to inject 1000ms latency and 429 status code with 50% chance
    errorChance: 0.5
    errorCode: 429
    errorBody: '{"error":"too_many_requests","message":"Rate limit exceeded"}'
    latencyMs: 1000

  - name: "sql-user-query-deadlock"
    type: "sql"
    target: "users"
    enabled: false
    errorChance: 0.2
    errorMessage: "ERROR: deadlock detected"
    latencyMs: 500
```

---

## Deployment Architectures

### Docker Compose (Local Development)

The Docker Compose configuration provisions a 6-node Redis Cluster along with databases, event brokers, and reactive service containers to replicate a microservices environment.

```mermaid
flowchart TB
    classDef gateway fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,font-weight:bold
    classDef core fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1.5px
    classDef infra fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef obs fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px
    classDef event fill:#431407,stroke:#fb923c,color:#fed7aa,stroke-width:1.5px

    subgraph DockerCompose["docker-compose.yml — Local Environment"]

        subgraph Gateway["API Gateway"]
            NGINX["NGINX Proxy :80"]:::gateway
            APIGW["API Gateway Container<br/>Vert.x REST Gateway :5000"]:::gateway
        end

        subgraph Services["Core Service Containers"]
            subgraph Identity["Identity & Access"]
                AUTH["auth-service"]:::core
                USER["user-service"]:::core
                ROLE["role-service"]:::core
            end

            subgraph MerchantSuite["Merchant Domain"]
                MERCH["merchant-service"]:::core
            end

            subgraph FinanceSuite["Finance & Card"]
                CARD["card-service"]:::core
                SALDO["saldo-service"]:::core
            end

            subgraph MovementsSuite["Fund Movements"]
                TOPUP["topup-service"]:::core
                TXN["transaction-service"]:::core
                TRANSFER["transfer-service"]:::core
                WITHDRAW["withdraw-service"]:::core
            end
        end

        subgraph Infra["Infrastructure Suite"]
            PG[("PostgreSQL :5432")]:::infra
            PGB[("PgBouncer :6432")]:::infra
            REDIS_CLUSTER[("Redis Cluster :6379-6384<br/>6 Nodes Enabled")]:::infra
            KAFKA[("Kafka Broker :9092")]:::infra
        end

        subgraph Obs["Observability Stack"]
            PROM["Prometheus :9090"]:::obs
            GRAFANA["Grafana :3000"]:::obs
            LOKI["Loki :3100"]:::obs
            JAEGER["Jaeger :16686"]:::obs
            OTEL["OTel Collector :4317"]:::obs
            NODEX["Node Exporter"]:::obs
            KAFKAX["Kafka Exporter"]:::obs
            PGX["Postgres Exporter"]:::obs
            PROMTAIL["Promtail Log Shipper"]:::obs
        end

        subgraph Events["Event Consumers"]
            EMAIL["Email Worker"]:::event
        end
    end

    NGINX --> APIGW
    
    APIGW -->|gRPC| AUTH
    APIGW -->|gRPC| USER
    APIGW -->|gRPC| ROLE
    APIGW -->|gRPC| MERCH
    APIGW -->|gRPC| CARD
    APIGW -->|gRPC| SALDO
    APIGW -->|gRPC| TOPUP
    APIGW -->|gRPC| TXN
    APIGW -->|gRPC| TRANSFER
    APIGW -->|gRPC| WITHDRAW

    AUTH -->|SQL| PGB
    USER -->|SQL| PGB
    ROLE -->|SQL| PGB
    MERCH -->|SQL| PGB
    CARD -->|SQL| PGB
    SALDO -->|SQL| PGB
    TOPUP -->|SQL| PGB
    TXN -->|SQL| PGB
    TRANSFER -->|SQL| PGB
    WITHDRAW -->|SQL| PGB

    PGB --> PG

    AUTH -->|Cache| REDIS_CLUSTER
    USER -->|Cache| REDIS_CLUSTER
    ROLE -->|Cache| REDIS_CLUSTER
    MERCH -->|Cache| REDIS_CLUSTER
    CARD -->|Cache| REDIS_CLUSTER
    SALDO -->|Cache| REDIS_CLUSTER
    APIGW --> REDIS_CLUSTER

    AUTH -->|gRPC| USER
    AUTH -->|gRPC| ROLE
    MERCH -->|gRPC| USER
    CARD -->|gRPC| USER
    TOPUP -->|gRPC| SALDO
    TOPUP -->|gRPC| TXN
    TRANSFER -->|gRPC| CARD
    TRANSFER -->|gRPC| SALDO
    TRANSFER -->|gRPC| TXN
    WITHDRAW -->|gRPC| CARD
    WITHDRAW -->|gRPC| SALDO
    WITHDRAW -->|gRPC| TXN

    TOPUP -->|Events| KAFKA
    TRANSFER -->|Events| KAFKA
    WITHDRAW -->|Events| KAFKA

    KAFKA --> EMAIL

    AUTH -.->|"Metrics"| PROM
    USER -.->|"Metrics"| PROM
    ROLE -.->|"Metrics"| PROM
    MERCH -.->|"Metrics"| PROM
    CARD -.->|"Metrics"| PROM
    SALDO -.->|"Metrics"| PROM
    TOPUP -.->|"Metrics"| PROM
    TXN -.->|"Metrics"| PROM
    TRANSFER -.->|"Metrics"| PROM
    WITHDRAW -.->|"Metrics"| PROM
    APIGW -.->|"Metrics"| PROM

    AUTH -.->|"Traces"| OTEL
    USER -.->|"Traces"| OTEL
    ROLE -.->|"Traces"| OTEL
    MERCH -.->|"Traces"| OTEL
    CARD -.->|"Traces"| OTEL
    SALDO -.->|"Traces"| OTEL
    TOPUP -.->|"Traces"| OTEL
    TXN -.->|"Traces"| OTEL
    TRANSFER -.->|"Traces"| OTEL
    WITHDRAW -.->|"Traces"| OTEL
    APIGW -.->|"Traces"| OTEL

    AUTH -.->|"Metrics"| PROM
    USER -.->|"Metrics"| PROM
    ROLE -.->|"Metrics"| PROM
    MERCH -.->|"Metrics"| PROM
    CARD -.->|"Metrics"| PROM
    SALDO -.->|"Metrics"| PROM
    TOPUP -.->|"Metrics"| PROM
    TXN -.->|"Metrics"| PROM
    TRANSFER -.->|"Metrics"| PROM
    WITHDRAW -.->|"Metrics"| PROM
    APIGW -.->|"Metrics"| PROM

    AUTH -.->|"Traces"| OTEL
    USER -.->|"Traces"| OTEL
    ROLE -.->|"Traces"| OTEL
    MERCH -.->|"Traces"| OTEL
    CARD -.->|"Traces"| OTEL
    SALDO -.->|"Traces"| OTEL
    TOPUP -.->|"Traces"| OTEL
    TXN -.->|"Traces"| OTEL
    TRANSFER -.->|"Traces"| OTEL
    WITHDRAW -.->|"Traces"| OTEL
    APIGW -.->|"Traces"| OTEL

    OTEL -.-> JAEGER
    PROMTAIL -.-> LOKI
    PROM -.-> GRAFANA
    LOKI -.-> GRAFANA

    KAFKA -.-> KAFKAX
    PG -.-> PGX
    KAFKAX -.-> PROM
    PGX -.-> PROM
    NODEX -.-> PROM
```

---

### Kubernetes (Production Clustering)

The production-grade Kubernetes architecture is designed for high availability, fault tolerance, and seamless horizontal scaling. All manifests are defined inside the custom `payment-gateway` namespace, route edge traffic using NGINX pods acting as a LoadBalancer, and manage service scalability using individual HPAs.

```mermaid
flowchart TB
    classDef client fill:#0f172a,stroke:#38bdf8,color:#e0f2fe,stroke-width:2px,font-weight:bold
    classDef ingress fill:#0f172a,stroke:#06b6d4,color:#e0f7fa,stroke-width:2px,font-weight:bold
    classDef k8sSvc fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2px,font-weight:bold
    classDef pod fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1.5px
    classDef stateful fill:#172554,stroke:#60a5fa,color:#dbeafe,stroke-width:1.5px
    classDef hpa fill:#064e3b,stroke:#34d399,color:#ecfdf5,stroke-width:1px,stroke-dasharray: 5 5
    classDef obs fill:#052e16,stroke:#4ade80,color:#dcfce7,stroke-width:1.5px

    Client["Client Applications<br/>(HTTPS Requests)"]:::client

    subgraph K8sCluster["Kubernetes Cluster — Namespace: payment-gateway"]
        direction TB

        subgraph IngressLayer["Edge Reverse Proxy (NGINX)"]
            NGINX_SVC["nginx-service<br/>(LoadBalancer :80)"]:::k8sSvc
            NGINX_POD["nginx-pods"]:::pod
        end

        subgraph GatewayServices["REST API Gateway (Scalable Deployment)"]
            APIGW_SVC["apigateway-service<br/>(ClusterIP :5000)"]:::k8sSvc
            APIGW_PODS["apigateway-pods"]:::pod
            APIGW_HPA["apigateway-hpa"]:::hpa
        end

        subgraph DomainServices["Internal gRPC Microservices"]
            direction TB
            
            subgraph IdentityZone["Identity Suite"]
                AUTH_POD["auth-pods"]:::pod
                USER_POD["user-pods"]:::pod
                ROLE_POD["role-pods"]:::pod
                AUTH_SVC["auth-service (gRPC)"]:::k8sSvc
                USER_SVC["user-service (gRPC)"]:::k8sSvc
                ROLE_SVC["role-service (gRPC)"]:::k8sSvc
            end

            subgraph MerchantZone["Merchant Suite"]
                MERCH_POD["merchant-pods"]:::pod
                MERCH_SVC["merchant-service (gRPC)"]:::k8sSvc
            end

            subgraph FinanceZone["Finance & Cards"]
                CARD_POD["card-pods"]:::pod
                SALDO_POD["saldo-pods"]:::pod
                CARD_SVC["card-service (gRPC)"]:::k8sSvc
                SALDO_SVC["saldo-service (gRPC)"]:::k8sSvc
            end

            subgraph MovementsZone["Ledgers & Fund Flows"]
                TOPUP_POD["topup-pods"]:::pod
                TX_POD["transaction-pods"]:::pod
                TRANSFER_POD["transfer-pods"]:::pod
                WITHDRAW_POD["withdraw-pods"]:::pod
                TOPUP_SVC["topup-service (gRPC)"]:::k8sSvc
                TX_SVC["transaction-service (gRPC)"]:::k8sSvc
                TRANSFER_SVC["transfer-service (gRPC)"]:::k8sSvc
                WITHDRAW_SVC["withdraw-service (gRPC)"]:::k8sSvc
            end
            
            PodsHPA["Domain Services HPAs<br/>(auth, card, merchant, etc.)"]:::hpa
        end

        subgraph DataObservability["Infrastructure & Databases"]
            PGB_SVC["pgbouncer-service<br/>(ClusterIP :6432)"]:::k8sSvc
            PGB_POD["pgbouncer-pods"]:::pod

            PG_SVC["postgres-service<br/>(ClusterIP :5432)"]:::k8sSvc
            PG_POD["postgres-pods"]:::pod
            
            REDIS_SVC["redis-cluster-service<br/>(ClusterIP :6379)"]:::k8sSvc
            REDIS_SET[("redis-cluster StatefulSet<br/>(6-Node Shards)")]:::stateful
            
            KAFKA_SVC["kafka-service<br/>(ClusterIP :9092)"]:::k8sSvc
            KAFKA_POD["kafka-pods"]:::pod
        end

        subgraph BackgroundWorkers["Event Consumers"]
            EMAIL_SVC["email-service<br/>(ClusterIP)"]:::k8sSvc
            EMAIL_PODS["email-pods"]:::pod
            EMAIL_HPA["email-hpa"]:::hpa
        end

        subgraph K8sObs["Observability Namespace Suite"]
            PROM_SVC["prometheus-service<br/>(ClusterIP :9090)"]:::k8sSvc
            PROM_POD["prometheus-pod"]:::pod

            OTEL_SVC["otel-collector-service<br/>(ClusterIP :4317)"]:::k8sSvc
            OTEL_POD["otel-collector-pod"]:::pod

            LOKI_SVC["loki-service<br/>(ClusterIP :3100)"]:::k8sSvc
            LOKI_POD["loki-pod"]:::pod

            JAEGER_SVC["jaeger-service<br/>(ClusterIP :16686)"]:::k8sSvc
            JAEGER_POD["jaeger-pod"]:::pod

            GRAFANA_SVC["grafana-service<br/>(ClusterIP :3000)"]:::k8sSvc
            GRAFANA_POD["grafana-pod"]:::pod

            ALERTMGR_SVC["alertmanager-service<br/>(ClusterIP :9093)"]:::k8sSvc
            ALERTMGR_POD["alertmanager-pod"]:::pod

            PROMTAIL["promtail-daemonset"]:::pod
            
            KAFKAX_SVC["kafka-exporter-service"]:::k8sSvc
            KAFKAX_POD["kafka-exporter-pod"]:::pod

            NODEX_SVC["node-exporter-service"]:::k8sSvc
            NODEX_POD["node-exporter-daemonset"]:::pod
        end
    end

    Client -->|HTTPS :443| NGINX_SVC
    NGINX_SVC --> NGINX_POD
    NGINX_POD -->|Proxy Pass| APIGW_SVC
    APIGW_SVC --> APIGW_PODS
    APIGW_HPA -.->|Autoscales| APIGW_PODS

    APIGW_PODS -->|gRPC call| AUTH_SVC
    APIGW_PODS -->|gRPC call| USER_SVC
    APIGW_PODS -->|gRPC call| ROLE_SVC
    APIGW_PODS -->|gRPC call| MERCH_SVC
    APIGW_PODS -->|gRPC call| CARD_SVC
    APIGW_PODS -->|gRPC call| SALDO_SVC
    APIGW_PODS -->|gRPC call| TOPUP_SVC
    APIGW_PODS -->|gRPC call| TX_SVC
    APIGW_PODS -->|gRPC call| TRANSFER_SVC
    APIGW_PODS -->|gRPC call| WITHDRAW_SVC
    
    AUTH_SVC --> AUTH_POD
    USER_SVC --> USER_POD
    ROLE_SVC --> ROLE_POD
    MERCH_SVC --> MERCH_POD
    CARD_SVC --> CARD_POD
    SALDO_SVC --> SALDO_POD
    TOPUP_SVC --> TOPUP_POD
    TX_SVC --> TX_POD
    TRANSFER_SVC --> TRANSFER_POD
    WITHDRAW_SVC --> WITHDRAW_POD

    AUTH_POD -->|SQL| PGB_SVC
    USER_POD -->|SQL| PGB_SVC
    ROLE_POD -->|SQL| PGB_SVC
    MERCH_POD -->|SQL| PGB_SVC
    CARD_POD -->|SQL| PGB_SVC
    SALDO_POD -->|SQL| PGB_SVC
    TOPUP_POD -->|SQL| PGB_SVC
    TX_POD -->|SQL| PGB_SVC
    TRANSFER_POD -->|SQL| PGB_SVC
    WITHDRAW_POD -->|SQL| PGB_SVC

    PGB_SVC --> PGB_POD
    PGB_POD -->|SQL| PG_SVC
    PG_SVC --> PG_POD

    AUTH_POD -->|Cache| REDIS_SVC
    USER_POD -->|Cache| REDIS_SVC
    ROLE_POD -->|Cache| REDIS_SVC
    MERCH_POD -->|Cache| REDIS_SVC
    CARD_POD -->|Cache| REDIS_SVC
    SALDO_POD -->|Cache| REDIS_SVC

    REDIS_SVC --> REDIS_SET

    AUTH_POD -->|gRPC| USER_SVC
    AUTH_POD -->|gRPC| ROLE_SVC
    MERCH_POD -->|gRPC| USER_SVC
    CARD_POD -->|gRPC| USER_SVC
    TOPUP_POD -->|gRPC| SALDO_SVC
    TOPUP_POD -->|gRPC| TX_SVC
    TRANSFER_POD -->|gRPC| CARD_SVC
    TRANSFER_POD -->|gRPC| SALDO_SVC
    TRANSFER_POD -->|gRPC| TX_SVC
    WITHDRAW_POD -->|gRPC| CARD_SVC
    WITHDRAW_POD -->|gRPC| SALDO_SVC
    WITHDRAW_POD -->|gRPC| TX_SVC

    TOPUP_POD -->|Events| KAFKA_SVC
    TRANSFER_POD -->|Events| KAFKA_SVC
    WITHDRAW_POD -->|Events| KAFKA_SVC

    KAFKA_SVC --> KAFKA_POD
    KAFKA_POD -->|Message Stream| EMAIL_SVC
    EMAIL_SVC --> EMAIL_PODS
    EMAIL_HPA -.->|Autoscales| EMAIL_PODS

    PodsHPA -.->|Autoscales| AUTH_POD
    PodsHPA -.->|Autoscales| USER_POD
    PodsHPA -.->|Autoscales| ROLE_POD
    PodsHPA -.->|Autoscales| MERCH_POD
    PodsHPA -.->|Autoscales| CARD_POD
    PodsHPA -.->|Autoscales| SALDO_POD
    PodsHPA -.->|Autoscales| TOPUP_POD
    PodsHPA -.->|Autoscales| TX_POD
    PodsHPA -.->|Autoscales| TRANSFER_POD
    PodsHPA -.->|Autoscales| WITHDRAW_POD

    AUTH_POD -.->|"Metrics"| PROM_SVC
    USER_POD -.->|"Metrics"| PROM_SVC
    ROLE_POD -.->|"Metrics"| PROM_SVC
    MERCH_POD -.->|"Metrics"| PROM_SVC
    CARD_POD -.->|"Metrics"| PROM_SVC
    SALDO_POD -.->|"Metrics"| PROM_SVC
    TOPUP_POD -.->|"Metrics"| PROM_SVC
    TX_POD -.->|"Metrics"| PROM_SVC
    TRANSFER_POD -.->|"Metrics"| PROM_SVC
    WITHDRAW_POD -.->|"Metrics"| PROM_SVC
    APIGW_PODS -.->|"Metrics"| PROM_SVC

    AUTH_POD -.->|"Traces"| OTEL_SVC
    USER_POD -.->|"Traces"| OTEL_SVC
    ROLE_POD -.->|"Traces"| OTEL_SVC
    MERCH_POD -.->|"Traces"| OTEL_SVC
    CARD_POD -.->|"Traces"| OTEL_SVC
    SALDO_POD -.->|"Traces"| OTEL_SVC
    TOPUP_POD -.->|"Traces"| OTEL_SVC
    TX_POD -.->|"Traces"| OTEL_SVC
    TRANSFER_POD -.->|"Traces"| OTEL_SVC
    WITHDRAW_POD -.->|"Traces"| OTEL_SVC
    APIGW_PODS -.->|"Traces"| OTEL_SVC

    PROM_SVC --> PROM_POD
    OTEL_SVC --> OTEL_POD
    LOKI_SVC --> LOKI_POD
    JAEGER_SVC --> JAEGER_POD
    GRAFANA_SVC --> GRAFANA_POD
    ALERTMGR_SVC --> ALERTMGR_POD

    OTEL_POD -.-> JAEGER_SVC
    PROMTAIL -.-> LOKI_SVC
    PROM_POD -.-> GRAFANA_SVC
    LOKI_POD -.-> GRAFANA_SVC
    PROM_POD -.-> ALERTMGR_SVC

    KAFKA_SVC -.-> KAFKAX_SVC
    KAFKAX_SVC --> KAFKAX_POD
    KAFKAX_POD -.-> PROM_SVC
    NODEX_SVC --> NODEX_POD
    NODEX_SVC -.-> PROM_SVC
```

### ArgoCD App-of-Apps GitOps Architecture

The platform follows GitOps best practices using ArgoCD for declarative continuous deployments. Replicating the App-of-Apps design pattern, a root Application (`payment-gateway-root`) automatically manages and tracks the states of individual child Applications mapping to Kustomize bases.

Sync waves (`argocd.argoproj.io/sync-wave` annotations) are strictly defined to guarantee database migrations run and complete before domain applications start.

```mermaid
graph TD
    classDef root fill:#1e293b,stroke:#22d3ee,color:#cffafe,stroke-width:2.5px,font-weight:bold
    classDef proj fill:#0f172a,stroke:#38bdf8,color:#e0f2fe,stroke-width:2px
    classDef app fill:#1e1b4b,stroke:#a78bfa,color:#ede9fe,stroke-width:1.5px
    classDef wave fill:#1c1917,stroke:#f59e0b,color:#fef3c7,stroke-width:1.5px
    classDef base fill:#052e16,stroke:#34d399,color:#dcfce7,stroke-width:1.5px

    RootApp["payment-gateway-root<br/>(ArgoCD Root Application)"]:::root
    AppProj["payment-gateway<br/>(ArgoCD AppProject)"]:::proj

    RootApp -->|Creates & Tracks| AppProj
    RootApp -->|Deploys Application Manifests| AppIndex["Child Applications List<br/>(deployments/gitops/argocd/apps/)"]:::app

    subgraph SyncWaves["Ordered Deployment Sequencing (Sync Waves 1 - 6)"]
        direction TB

        subgraph Wave1["Wave 1: Namespace & Infrastructure"]
            W1_CM["common"]:::wave
            W1_PG["infra-postgres"]:::wave
            W1_RD["infra-redis"]:::wave
            W1_KF["infra-kafka"]:::wave
        end

        subgraph Wave2["Wave 2: Database Migration"]
            W2_MIG["db-migration"]:::wave
        end

        subgraph Wave3["Wave 3: Core Services & Gateway Pooler"]
            W3_PGB["pgbouncer"]:::wave
            W3_AUTH["service-auth"]:::wave
            W3_USR["service-user"]:::wave
            W3_ROL["service-role"]:::wave
            W3_CRD["service-card"]:::wave
            W3_MER["service-merchant"]:::wave
            W3_SLD["service-saldo"]:::wave
            W3_EML["service-email"]:::wave
        end

        subgraph Wave4["Wave 4: Financial Movements"]
            W4_TOP["service-topup"]:::wave
            W4_TRF["service-transfer"]:::wave
            W4_WIT["service-withdraw"]:::wave
            W4_TXN["service-transaction"]:::wave
        end

        subgraph Wave5["Wave 5: Reverse Proxy Gateway"]
            W5_APIGW["apigateway"]:::wave
            W5_NGINX["nginx"]:::wave
        end

        subgraph Wave6["Wave 6: Observability Suite"]
            W6_OBS["service-observability"]:::wave
        end

        Wave1 -->|Triggers next wave| Wave2
        Wave2 -->|Triggers next wave| Wave3
        Wave3 -->|Triggers next wave| Wave4
        Wave4 -->|Triggers next wave| Wave5
        Wave5 -->|Triggers next wave| Wave6
    end

    AppIndex -->|Deploys| Wave1
    AppIndex -->|Deploys| Wave2
    AppIndex -->|Deploys| Wave3
    AppIndex -->|Deploys| Wave4
    AppIndex -->|Deploys| Wave5
    AppIndex -->|Deploys| Wave6

    subgraph K8sBases["Target: Kustomize Base Resources"]
        B_COMMON["deployments/kubernetes/base/common"]:::base
        B_PG["deployments/kubernetes/base/postgres"]:::base
        B_RD["deployments/kubernetes/base/redis"]:::base
        B_KF["deployments/kubernetes/base/kafka"]:::base
        B_MIG["deployments/kubernetes/base/db-migration"]:::base
        B_PGB["deployments/kubernetes/base/pgbouncer"]:::base
        B_AUTH["deployments/kubernetes/base/auth"]:::base
        B_USR["deployments/kubernetes/base/user"]:::base
        B_ROL["deployments/kubernetes/base/role"]:::base
        B_CRD["deployments/kubernetes/base/card"]:::base
        B_MER["deployments/kubernetes/base/merchant"]:::base
        B_SLD["deployments/kubernetes/base/saldo"]:::base
        B_EML["deployments/kubernetes/base/email"]:::base
        B_TOP["deployments/kubernetes/base/topup"]:::base
        B_TRF["deployments/kubernetes/base/transfer"]:::base
        B_WIT["deployments/kubernetes/base/withdraw"]:::base
        B_TXN["deployments/kubernetes/base/transaction"]:::base
        B_APIGW["deployments/kubernetes/base/apigateway"]:::base
        B_NGINX["deployments/kubernetes/base/nginx"]:::base
        B_OBS["deployments/kubernetes/base/observability"]:::base
    end

    W1_CM -->|Reconciles| B_COMMON
    W1_PG -->|Reconciles| B_PG
    W1_RD -->|Reconciles| B_RD
    W1_KF -->|Reconciles| B_KF
    W2_MIG -->|Reconciles| B_MIG
    W3_PGB -->|Reconciles| B_PGB
    W3_AUTH -->|Reconciles| B_AUTH
    W3_USR -->|Reconciles| B_USR
    W3_ROL -->|Reconciles| B_ROL
    W3_CRD -->|Reconciles| B_CRD
    W3_MER -->|Reconciles| B_MER
    W3_SLD -->|Reconciles| B_SLD
    W3_EML -->|Reconciles| B_EML
    W4_TOP -->|Reconciles| B_TOP
    W4_TRF -->|Reconciles| B_TRF
    W4_WIT -->|Reconciles| B_WIT
    W4_TXN -->|Reconciles| B_TXN
    W5_APIGW -->|Reconciles| B_APIGW
    W5_NGINX -->|Reconciles| B_NGINX
    W6_OBS -->|Reconciles| B_OBS
```

---

## Technology Stack

| Category | Selected Technologies | Purpose |
| :--- | :--- | :--- |
| **Language** | Java 21 (Eclipse Vert.x v4.5.24) | Reactive, non-blocking asynchronous Java execution. |
| **API Edge Gateway** | Vert.x Web Router | Reactive REST API Gateway router and reverse proxy destination. |
| **RPC Inter-service** | Vert.x gRPC Client & Server | Blazing fast, contract-first synchronous gRPC communication. |
| **Database** | PostgreSQL v17 | Safe ACID ledger persistent storage system. |
| **Database Gateway** | PgBouncer | Extreme-efficiency PostgreSQL socket connection pooler. |
| **DB Migrations** | Flyway | Incremental database schema version manager run on startup. |
| **Caching Tier** | Redis Cluster (6 Nodes) | Resilient, distributed key-value cache layer. |
| **Messaging Stream** | Apache Kafka | Asynchronous high-throughput messaging event bus (KRaft mode). |
| **Token Manager** | JWT | Secure stateless request authentication standard. |
| **Observability** | OpenTelemetry + Jaeger | Vendor-neutral distributed telemetry pipeline and visualization. |
| **Docker Engine** | Compose | Local environment virtualization orchestration. |
| **Orchestrator** | Kubernetes | Production-scale auto-scaling pod clustering infrastructure. |

---

## CI/CD & DevSecOps

The platform enforces strict DevSecOps practices using automated verification and security scanning workflows:

### GitHub Actions Pipeline
The automated workflow in [.github/workflows/ci.yml](file:///.github/workflows/ci.yml) triggers on every push and pull request to the `main` or `master` branches, performing the following steps:
1. **Compilation Check**: Sets up JDK 21 (Temurin) with Maven caching to build the Java code.
2. **Docker Image Builds**: Builds the 13 microservice images via Docker Buildx (utilizing BuildKit cache mounts to optimize build times).
3. **Trivy Filesystem & Secret Scan**: Audits dependencies for CVE vulnerabilities and scans files for exposed keys/secrets.
4. **Trivy IaC Audit**: Audits Kubernetes manifests and Docker Compose configurations for security misconfigurations.
5. **Trivy Image Scan**: Scans the built container images (e.g., `auth:latest`) for vulnerabilities.

Any high or critical findings will automatically fail the build, preventing insecure code or credentials from merging.

### Local Security Scanning
You can audit your local workspace (vulnerabilities, hardcoded secrets, and Kubernetes/Compose configurations) using our pre-configured Trivy Docker scanner script:
```bash
./deployments/trivy/scan.sh
```

For more details on local scanning and deploying the **Trivy Kubernetes Operator** to monitor runtime workloads in your cluster, refer to the [Trivy DevSecOps Guide](file:///deployments/trivy/README.md).

---

## Getting Started

### Prerequisites

Ensure the following system packages are locally configured:

- [Git](https://git-scm.com/)
- [Java Development Kit (JDK 21+)](https://adoptium.net/)
- [Apache Maven](https://maven.org/) (v3.9+)
- [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- [Protobuf Compiler](https://grpc.io/docs/protoc-installation/) (optional)

### 1. Clone the Workspace

```sh
git clone https://github.com/MamangRust/modular-monolith-vertx-payment-gateway.git
cd modular-monolith-vertx-payment-gateway
```

### 2. Prepare Environment Configurations

Setup the system configurations from placeholders:

```sh
# Copy root variables
cp .env.example .env

# Copy local docker settings overrides
cp deployments/local/docker.env.example deployments/local/docker.env
```

### 3. Build the Maven Project

Compile all submodules and build the executable JAR files:

```sh
mvn clean install
```

### 4. Build Docker Images and Start Environment

Use the included build script to compile the service Docker images, then boot the Docker Compose stack:

```sh
# Build docker images for all services
./build-docker-images.sh

# Start local infrastructure, telemetry containers, and application services
docker-compose -f deployments/local/docker-compose.yml up -d
```

Flyway database migrations run automatically on service startup, preparing the database schema.

To verify the cluster services are up and healthy:

```sh
docker-compose -f deployments/local/docker-compose.yml ps
```

---

## Port Map Registry

| Application/Service | Port Configuration / URL |
| :--- | :--- |
| **NGINX Reverse Proxy Edge** | [http://localhost](http://localhost) |
| **API Gateway Direct REST Hub** | [http://localhost:5000](http://localhost:5000) |
| **Grafana Dashboard Portal** | [http://localhost:3000](http://localhost:3000) *(Credentials: `admin`/`admin`)* |
| **Prometheus Telemetry** | [http://localhost:9090](http://localhost:9090) |
| **Jaeger Distributed Tracing** | [http://localhost:16686](http://localhost:16686) |
| **PgBouncer Gateway Node** | `localhost:6432` |
| **PostgreSQL Database Engine** | `localhost:5432` |

To stop the development system and clean up resources:

```sh
docker-compose -f deployments/local/docker-compose.yml down -v
```

---

## Maven & Shell Commands Reference

| Command | Scope |
| :--- | :--- |
| `mvn clean install` | Cleans target directories, runs tests, compiles all submodules, and generates package JARs. |
| `mvn compile` | Compiles raw Java source files for all modules. |
| `./build-docker-images.sh` | Orchestrates the build of Docker images for all Vert.x microservices. |
| `docker-compose -f deployments/local/docker-compose.yml up -d` | Launches all containers (DBs, Redis cluster, Kafka, observability, and Java services) in background mode. |
| `docker-compose -f deployments/local/docker-compose.yml down` | Stops compose containers, releasing standard networks. |
| `docker-compose -f deployments/local/docker-compose.yml logs -f <service>` | Follows the realtime stdout logs of a specific service container. |

---

## Workspace Directory Tree

```
vertx-payment-gateway/
├── pom.xml                         # Root Maven Parent POM
├── chaos.yaml                      # Chaos Engineering policies configuration file
├── proto/                          # Protobuf contracts (12 domains)
│   ├── auth.proto                  #   Identity tokens contracts
│   ├── card/                       #   Virtual Card specifications
│   ├── common/                     #   Shared protobuf data types
│   ├── merchant/                   #   Merchant account declarations
│   ├── merchant_document/          #   Verification files specifications
│   ├── role/                       #   Role mapping specifications
│   ├── saldo/                      #   Balance updates specifications
│   ├── topup/                      #   Funding balance specifications
│   ├── transaction/                #   General audit register specifications
│   ├── transfer/                   #   Peer-to-peer transaction specifications
│   ├── user/                       #   User CRUD data properties
│   └── withdraw/                   #   Bank settlement configurations
├── common/                         # Shared Maven library Module
│   └── src/main/java/io/example/common/
│       ├── config/                 #   AppConfig, JwtConfig, RedisConfig, FlywayConfig
│       ├── chaos/                  #   Chaos Engineering engine (Manager, SQL proxy, HTTP middleware, resource sabotage)
│       ├── observability/          #   TracingMetrics config
│       ├── service/                #   RedisService utilities
│       └── pb/                     #   Compiled Java Protobuf gRPC stubs
├── apigateway/                     # REST API Gateway (Vert.x Router proxying to gRPC)
├── auth/                           # Authentication engine service
├── user/                           # User profiles service (CQRS)
├── role/                           # RBAC authorization service
├── merchant/                       # Merchant onboarding & reports service
├── card/                           # Virtual Card & Analytics service
├── saldo/                          # Real-time balance ledger service
├── topup/                          # Funding ledger service
├── transaction/                    # Central transaction ledger audit service
├── transfer/                       # P2P fund transfer service
├── withdraw/                       # Outbound bank settlement service
├── email/                          # Asynchronous Kafka notifications service
├── db-migration/                   # Standalone database migration runner (Flyway)
├── deployments/
│   ├── local/                      #   Docker compose infrastructure files
│   ├── kubernetes/                 #   Production K8s deployment bases
│   └── gitops/                     #   ArgoCD App-of-Apps configuration layouts
├── observability/                  #   Telemetry pipelines configurations (Loki, OTEL, Alertmanager)
├── grafana/                        #   Pre-configured dashboard JSON files
├── nginx/                          #   Reverse-proxy NGINX rules
└── images/                         #   Architecture diagrams & dashboard screenshots
```

---

## License

This project is open-sourced under the MIT License for educational and development purposes.

---

<p align="center">
  Built with Java, Eclipse Vert.x, gRPC, Apache Kafka, and a passion for high-performance reactive modular monoliths.
</p>