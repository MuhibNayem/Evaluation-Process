# Evaluation Service

A high-performance, scalable microservice for managing evaluation campaigns, submissions, and reporting. Built with **Java 25** and **Spring Boot 4**, leveraging virtual threads for optimal concurrency and a hexagonal architecture for maintainability.

## 🚀 Key Features

*   **Dynamic Campaign Management**: Create, activate, and manage evaluation campaigns with customizable templates and scoring rules.
*   **Flexible Templates**: Define evaluation structures with weighted sections, custom formulas, and version control.
*   **Advanced Scoring**: Support for weighted averages, custom formulas, and partial credit.
*   **Real-time Reporting**: Generate individual and campaign-level reports with CSV and PDF export capabilities.
*   **System-Wide Configuration**: Admin-configurable settings with campaign-level overrides.
*   **High Performance**: Utilizes **Java 25 Virtual Threads** for non-blocking I/O and **Redis** for caching.
*   **Security**: Role-based access control (RBAC) secured with JWT authentication.
*   **Resilience**: Circuit breakers (Resilience4j) and robust error handling for external integrations.

## 🏗️ Architecture

The service follows a **Hexagonal Architecture (Ports and Adapters)** to decouple business logic from infrastructure concerns.

*   **Domain Layer**: Core business logic, entities (`Campaign`, `Evaluation`, `Template`), and value objects. Dependency-free.
*   **Application Layer**: Use case implementations (`CampaignManagementService`, `EvaluationSubmissionService`) orchestrating domain objects.
*   **API Layer (Inbound Adapters)**: REST Controllers exposing functionality via HTTP.
*   **Infrastructure Layer (Outbound Adapters)**:
    *   **Persistence**: PostgreSQL with Spring Data JPA.
    *   **Caching**: Redis for session and entity caching.
    *   **Notifications**: Async Webhook integration via OpenFeign.
    *   **Configuration**: Dynamic properties via `SystemSettingsWithOverrides`.

## 🛠️ Technology Stack

*   **Language**: Java 25 (Virtual Threads, Records, Pattern Matching)
*   **Framework**: Spring Boot 4.0.1
*   **Database**: PostgreSQL 16
*   **Cache**: Redis 7
*   **Build Tool**: Gradle 8.5
*   **Observability**: Micrometer, Prometheus, Zipkin
*   **Testing**: JUnit 5, Testcontainers

## 📋 Prerequisites

*   **Java 25 SDK** installed
*   **Docker** and **Docker Compose**
*   **Gradle 8.x** (or use generic wrapper)

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone <repository-url>
cd evaluation-service
```

### 2. Start Infrastructure
Start PostgreSQL, Redis, and observability tools using Docker Compose:
```bash
docker-compose up -d postgres redis zipkin prometheus grafana
```

### 3. Build the Application
```bash
./gradlew clean build
```

### 4. Run the Application
```bash
java -jar build/libs/evaluation-service-1.0.0.jar
```
*Alternatively, run with Gradle:*
```bash
./gradlew bootRun
```

The application will start on **port 8080**.

## ⚙️ Configuration

The service is configured via `application.yml`. Key customizable properties:

| Property | Default | Description |
| :--- | :--- | :--- |
| `server.port` | `8080` | HTTP server port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/...` | Database URL |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.threads.virtual.enabled` | `true` | Enable Virtual Threads |
| `evaluation.service.features.enable-reports` | `true` | Enable report generation |
| `evaluation.service.notification.webhook-url` | *empty* | URL for webhook notifications |

### Dynamic System Settings
Admins can override specific defaults at runtime without restarting via the `/api/v1/admin/settings` endpoints.

## 📖 API Documentation

The service exposes a comprehensive REST API. 
**[View Full API Documentation](API_DOCUMENTATION.md)**

### Core Endpoints
*   `GET /api/v1/campaigns`: List active campaigns
*   `POST /api/v1/evaluations`: Submit an evaluation
*   `GET /api/v1/reports/campaign/{id}`: Get campaign performance report
*   `POST /api/v1/templates`: Create a new evaluation template

## 🩺 Monitoring & Observability

*   **Health Check**: `GET /actuator/health`
*   **Metrics (Prometheus)**: `GET /actuator/prometheus`
*   **Info**: `GET /actuator/info`

## 📦 Deployment

### Docker
Build and run the containerized image:
```bash
docker build -t evaluation-service .
docker run -p 8080:8080 -e DB_PASSWORD=secret evaluation-service
```

### Kubernetes (Ready)
The application includes:
*   **Liveness Probe**: `/actuator/health/liveness`
*   **Readiness Probe**: `/actuator/health/readiness`
*   **Graceful Shutdown**: Enabled by default

## 🚨 Troubleshooting

*   **Database Connection Failed**: Ensure PostgreSQL is running and credentials in `application.yml` match.
*   **Redis Connection Refused**: generic `RedisConnectionException` usually means Redis container is not reachable. Check `docker ps`.
*   **Virtual Threads Not Used**: verify `spring.threads.virtual.enabled=true` and run on Java 21+.

## 🤝 Contributing

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/amazing-feature`).
3.  Commit your changes (`git commit -m 'Add some amazing feature'`).
4.  Push to the branch (`git push origin feature/amazing-feature`).
5.  Open a Pull Request.

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.