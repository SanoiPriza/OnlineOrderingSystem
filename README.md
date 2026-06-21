# Online Ordering System

## Overview
**Online Ordering System** is a robust, microservices-based e-commerce platform developed using Spring Boot, Spring Cloud, PostgreSQL, and RabbitMQ. It demonstrates advanced enterprise architecture patterns, including the **Transactional Outbox Pattern** for reliable messaging, distributed tracing, and centralized API routing. The system is designed to be scalable, fault-tolerant, and highly cohesive.

## Features & Architecture
- **Microservices Architecture**: The system is fully modularized into independent services (`UserService`, `ProductService`, `OrderService`, `PaymentService`, `AdminService`) coordinated via a central `DiscoveryService` (Eureka).
- **API Gateway**: Acts as the single entry point for all client requests, handling routing (including dedicated internal routes for the Admin dashboard), JWT token validation, and rate-limiting using Redis.
- **Asynchronous Communication**: Uses RabbitMQ for inter-service event-driven communication (e.g., `OrderCreatedEvent`, `PaymentRequestEvent`).
- **Transactional Outbox Pattern**: Implemented across services to solve the dual-write problem, ensuring 100% consistency between local database transactions and message broker publishing.
- **Idempotency**: Message consumers are protected against duplicate processing via dedicated `ProcessedEvent` tracking.
- **Distributed Tracing**: Integrated with Zipkin and Micrometer Tracing to easily monitor request flows across all microservices.
- **Security**: Stateless authentication utilizing JSON Web Tokens (JWT) verified at the gateway and propagated securely between internal services, backed by environment-aware configuration fallbacks.
- **Admin Dashboard**: Features an `AdminService` that monitors system health (via explicit API Gateway routing), aggregates snapshots, and provides Dead Letter Queue (DLQ) retry mechanisms.

## Technologies Used
- **Java 25**
- **Spring Boot 4.x**
- **Spring Cloud** (Netflix Eureka, API Gateway, OpenFeign)
- **Database**: PostgreSQL
- **Message Broker**: RabbitMQ
- **Caching / Rate Limiting**: Redis
- **Distributed Tracing**: Zipkin
- **Containerization**: Docker & Docker Compose (with robust Eureka service discovery networking)
- **Build Tool**: Maven (centralized BOM management ensuring secure dependencies like `commons-lang3`)

## Modules Breakdown
- `ApiGateway`: Handles external traffic, security, and routing.
- `DiscoveryService`: Service registry for dynamic service location.
- `ConfigServer`: Centralized configuration management.
- `UserService`: Manages user accounts and authentication.
- `ProductService`: Manages product catalog and inventory reservations.
- `OrderService`: Orchestrates the ordering process and temporal coupling fallbacks.
- `PaymentService`: Simulates external payment gateway integration.
- `AdminService`: Provides UI and APIs for system monitoring and DLQ management.
- `DomainCommon` & `SecurityCommon`: Shared DTOs, Exceptions, and Security Filters.

## Prerequisites
- **Java 25**
- **Maven**
- **Docker** & **Docker Compose**

## Installation & Running

### 1. Build the project
To build all microservices and shared libraries, run the following command in the root directory:
```bash
mvn clean install -DskipTests
```

### 2. Start Infrastructure and Services
The entire environment (Databases, RabbitMQ, Redis, Zipkin, and all microservices) is containerized and can be started via Docker Compose:
```bash
docker-compose up -d --build
```

### 3. Verify Deployment
Once all containers are running, you can access the following infrastructure endpoints:
- **Eureka Dashboard**: `http://localhost:8761`
- **RabbitMQ Management**: `http://localhost:15672` (guest / guest)
- **Zipkin UI**: `http://localhost:9411`
- **API Gateway**: `http://localhost:8080`

## Error Handling & Resiliency
The system employs best practices for enterprise resiliency:
- **Dead Letter Queues (DLQ)**: Failed messages are routed to DLQs and can be manually retried via the Admin Service.
- **Feign Client Fallbacks**: HTTP fallbacks ensure the system degrades gracefully if a dependent service is temporarily unavailable (e.g., retrieving live product prices if the local cache is stale).
- **Connection Pool Exhaustion Prevention**: Network-bound operations (like payment processing) are executed completely asynchronously, decoupled from primary database transactions.