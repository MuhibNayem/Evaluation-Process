# Microservices Architecture: Evaluation Service

## 1. Architectural Style
The **Evaluation Service** is designed as a self-contained, domain-centric microservice following the **Hexagonal Architecture (Ports and Adapters)** pattern. This ensures:
*   **Isolation**: Business logic is independent of frameworks, databases, and UI.
*   **Testability**: The core domain can be tested without external infrastructure.
*   **Modularity**: Features like "Campaigns" and "Scoring" are logically separated modules within the service.

---

## 2. System Boundaries & Context
The service operates within a Spring Cloud ecosystem, interacting with other components via standard protocols.

### External Dependencies
*   **Service Discovery**: Integrating with **Netflix Eureka** for dynamic registration.
*   **Configuration**: Fetches dynamic config from **Spring Cloud Config Server**.
*   **Identity Provider**: Relies on an external IAM (Keycloak/Auth0) to issue JWT tokens (stateless validation).
*   **Notification Targets**: Sends asynchronous webhooks to external systems (Slack, Email Service).

---

## 3. Internal Component Design (Hexagonal)

### The Core (Domain Layer)
*   **Entities**: `Campaign`, `Evaluation`, `Template`
*   **Rules**: `ScoringStrategy` (Weighted, Median, etc.)
*   **Events**: `CampaignClosedEvent`, `EvaluationSubmittedEvent`
*   *Characteristics*: Pure Java 25, no framework dependencies.

### The Application Layer (Ports)
*   **Input Ports (Use Cases)**: `CampaignManagementUseCase`, `EvaluationSubmissionUseCase`.
*   **Output Ports**: `CampaignPersistencePort`, `NotificationPort`.
*   **Services**: Orchestrates the flow (e.g., `CampaignManagementService` transactions).

### The Adapters (Infrastructure Layer)
*   **Driving Adapters (Inbound)**:
    *   **REST Controllers**: `CampaignController`, `EvaluationController`.
    *   **Schedule Adapter**: `CampaignScheduler` (triggers lifecycle events).
*   **Driven Adapters (Outbound)**:
    *   **Persistence**: `JpaCampaignAdapter` (PostgreSQL), `RedisCacheAdapter`.
    *   **Notification**: `WebhookNotificationAdapter` (Feign Client).

---

## 4. Technical Implementation

| Concern | Implementation | Rationale |
| :--- | :--- | :--- |
| **Concurrency** | **Java 25 Virtual Threads** | High-throughput blocking I/O (DB/Network) without Reactive complexity. |
| **Communication** | **REST + JSON** | Standard synchronous API. |
| **Resilience** | **Resilience4j** | Circuit Breakers for Webhook integrations. |
| **Inter-Service** | **Spring Cloud OpenFeign** | Declarative REST clients. |
| **Data Storage** | **PostgreSQL 16** | Relational integrity for complex evaluation schemas. |
| **Caching** | **Redis 7** | Sub-millisecond access for active templates and sessions. |
| **Observability** | **Micrometer + Zipkin** | Distributed tracing and Prometheus metrics. |

---

## 5. Data Flow Examples

### Submit Evaluation (Command)
1.  **API**: `POST /evaluations` receives JSON.
2.  **Controller**: Validates DTO, calls `submitEvaluation()` use case.
3.  **Service**:
    *   Loads `Campaign` and `Template` via Persistence Port.
    *   Validates Rules (Is campaign active? Is user assigned?).
    *   Calculates Score using `ScoringStrategy`.
    *   Saves `Evaluation` entity.
4.  **Event**: Publishes `EvaluationSubmittedEvent`.

### Auto-Close Campaign (Scheduled)
1.  **Scheduler**: `CampaignScheduler` wakes up (Cron).
2.  **Service**: Finds active campaigns past their `EndDate`.
3.  **Domain**: Calls `campaign.close()`.
4.  **Notification**: Triggers `WebhookNotificationAdapter` to alert admins.

---

## 6. Scalability Strategy
*   **Stateless**: The service stores no user session state; perfectly horizontally scalable behind a Load Balancer.
*   **Database**: Connection pooling (HikariCP) optimized for Virtual Threads.
*   **Caching**: Heavy read operations (fetching Templates/Campaigns) are cached in Redis to offload the DB.