<div align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Flutter-3.x-blue.svg?logo=flutter" alt="Flutter">
  <img src="https://img.shields.io/badge/React-18-61DAFB.svg?logo=react" alt="React">
  <img src="https://img.shields.io/badge/Kafka-Event%20Driven-black.svg?logo=apachekafka" alt="Kafka">
  <img src="https://img.shields.io/badge/PostgreSQL-TimescaleDB-336791.svg?logo=postgresql" alt="PostgreSQL">
  
  <h1>FleetIQ 🚛</h1>
  <p><strong>Enterprise-Grade Microservices Fleet Management Platform</strong></p>
</div>

---

## 🌐 Live Demo
The platform is currently live! You can access the React Admin Dashboard here:
👉 **[FleetIQ Live Dashboard](https://fleet-iq-lilac.vercel.app/)**

---

## 📖 Overview
FleetIQ is a massive, multi-tenant IoT fleet management platform designed for scale. It handles real-time vehicle telemetry ingestion (via MQTT & Kafka), complex spatial geofencing algorithms, AI-driven vehicle diagnostics, and dynamic driver safety scoring.

The platform is architected as an **Event-Driven Microservices Ecosystem** with 10 distinct Spring Boot backend services, a modular React Admin Dashboard, and an offline-first Flutter Mobile Monorepo for drivers and fleet managers.

---

## ⚡ Key Features
* **🚀 Real-Time Telemetry Processing**: High-throughput ingestion of GPS/OBD-II data using MQTT Mosquitto, standardized via a Device Gateway, and streamed through Apache Kafka.
* **📍 Spatial Geofencing (JTS R-Tree)**: In-memory R-Tree spatial indexing for lightning-fast geo-fencing (ENTER/EXIT events, speed limit enforcement, and dwell time tracking).
* **⛽ Advanced Fuel Analytics**: Sliding-window algorithms detecting fuel theft (sudden drops), slow leaks, and excessive idling using moving averages.
* **🛡️ Dynamic Driver Scoring**: Real-time event consumption (harsh braking, cornering) to dynamically adjust driver safety scores and dispatch coaching suggestions.
* **🩺 Vehicle Diagnostic Rules Engine**: Tenant-configurable sensor thresholds evaluating OBD-II telemetry (engine RPM, coolant temp, oil pressure) to predict breakdowns.
* **📱 Offline-First Mobile Apps**: Flutter driver app utilizing Drift (SQLite) to queue multi-point inspections and SOS alerts offline, syncing automatically when connectivity is restored.

---

## 🏗️ Architecture

FleetIQ is built using true Microservices principles, avoiding monolithic anti-patterns. Services communicate asynchronously via Kafka topics to decouple logic and improve fault tolerance.

### Microservices Breakdown
1. **`fleetiq-api-gateway`**: Spring Cloud Gateway routing rules, CORS, and centralized entry point.
2. **`fleetiq-auth-service`**: JWT generation and Role-Based Access Control (RBAC).
3. **`fleetiq-vehicle-service`**: Multi-tenant CRUD for vehicles, device assignments, and configurations.
4. **`fleetiq-device-gateway`**: MQTT protocol translator (TCP/UDP to JSON) pushing to `raw.telemetry`.
5. **`fleetiq-tracking-service`**: Processes raw Kafka streams into `processed.telemetry` and serves real-time WebSockets to the frontend map.
6. **`fleetiq-alerts-service`**: Geo-spatial engine evaluating boundaries and rules.
7. **`fleetiq-fuel-service`**: State-machine tracking fuel burn rates vs established baselines.
8. **`fleetiq-driver-service`**: Manages shift logs, driver scoring, and route assignments.
9. **`fleetiq-health-service`**: Diagnostic DTC code parsing and predictive maintenance work orders.
10. **`fleetiq-analytics-service`**: TimescaleDB continuous aggregates and scheduled PDF/CSV report generation.

---

## 🛠️ Technology Stack

| Category | Technologies |
|---|---|
| **Backend** | Java 21, Spring Boot 3.2.5, Spring Cloud, Hibernate / JPA, JTS Topology |
| **Messaging** | Apache Kafka, Eclipse Mosquitto (MQTT), WebSockets (STOMP) |
| **Databases** | PostgreSQL (PostGIS & TimescaleDB extensions), Redis (Caching) |
| **Frontend** | React, Vite, Recharts, Leaflet (Maps), Vanilla CSS (Design Tokens) |
| **Mobile** | Flutter, BLoC (State), Drift (SQLite), Dio, flutter_map |
| **DevOps** | Docker, Docker Compose, Kubernetes, GitHub Actions, Nginx |
| **Observability** | Prometheus, Grafana, Micrometer, Actuator |

---

## 🚀 How to Run (Local Development)

The entire enterprise architecture is configured to boot locally using a single command. 

### Prerequisites
- Docker & Docker Compose
- Maven (Optional, as CI/CD builds JARs)
- Node.js & Flutter SDK (for frontend/mobile modifications)

### Start the Platform
```bash
# Start infrastructure, 10 microservices, and React dashboard
docker compose up --build -d
```

### Access Points
- **Admin Dashboard**: `http://localhost:3000`
- **API Gateway**: `http://localhost:8080`
- **Grafana Metrics**: `http://localhost:3001`

---

## ☁️ Cloud Deployment Strategy (Production)
To maintain the microservice architecture without incurring massive cloud costs, the project utilizes a hybrid modern cloud stack:
* **Frontend**: Deployed on **Vercel** (`https://fleet-iq-lilac.vercel.app/`) for global CDN edge delivery.
* **Backend Services**: API Gateway, Auth, Vehicle, and Driver services deployed natively on **Render** using Docker containers.
* **Databases**: Serverless Postgres provided by **Neon**.
* **Messaging**: **Upstash Kafka** used for real-time secure event streaming (`raw.telemetry` and `processed.telemetry`).
* **CI/CD**: Fully automated deployments triggered via GitHub repository syncs (Render Blueprint `render.yaml`).

---

<div align="center">
  <i>Developed with focus on Clean Architecture, Event-Driven Design, and Enterprise Scalability.</i>
</div>
