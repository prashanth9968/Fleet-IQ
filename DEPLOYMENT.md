# FleetIQ Deployment Guide

## Mode 1: Local Development (Full Architecture)

The quickest way to spin up the entire FleetIQ microservices architecture locally is using Docker Compose. This starts all 10 Java backend services, the React admin dashboard, the API gateway, and all infrastructure dependencies (PostgreSQL, Kafka, Redis, MQTT, Prometheus, Grafana).

```bash
# Build and start all services in the background
docker compose up --build -d

# View logs for a specific service (e.g., the API Gateway)
docker compose logs -f api-gateway

# Shutdown and clean up volumes
docker compose down -v
```

### Accessing Services Locally
- **React Dashboard**: `http://localhost:3000`
- **API Gateway**: `http://localhost:8080`
- **Grafana (Metrics)**: `http://localhost:3001` (admin/admin)
- **Prometheus**: `http://localhost:9090`

---

## Mode 2: Cloud Demo Deployment (Hybrid Free Tier)

For portfolio demonstrations and interviews, deploying 10 individual microservices on a paid cloud provider is expensive, and free tiers will quickly exhaust limits. Instead, we use a hybrid deployment model.

### 1. Database & Infrastructure
- **PostgreSQL**: Create a free serverless Postgres database on **Neon** (`neon.tech`). Update the `SPRING_DATASOURCE_URL` secrets.
- **Redis**: Create a free Redis instance on **Upstash** (`upstash.com`). Update `SPRING_REDIS_HOST`.
- **MQTT**: Create a free cluster on **HiveMQ Cloud**.

### 2. Backend APIs (Render & Koyeb)
We retain the true microservice architecture. Do not merge into a monolith.
Deploy the services individually by linking your GitHub repository to Render/Koyeb web services.

**API Gateway (Render)**
- **Build Command**: `cd fleetiq-api-gateway && mvn clean package -DskipTests`
- **Start Command**: `java -jar target/*.jar`
- **Env Vars**: Set URLs for the downstream services once they are deployed.

**Downstream Services (Render/Koyeb)**
- **Auth, Vehicle, Driver, Health, Alerts**: Deploy as free web services on **Render**.
- **Tracking, Fuel**: Since Render limits instances, deploy these on **Koyeb** free tier.

### 3. Frontend (Netlify)
Deploy the React Admin Dashboard to Netlify.

- **Build Command**: `cd fleetiq-admin-dashboard && npm run build`
- **Publish Directory**: `fleetiq-admin-dashboard/dist`
- **Env Vars**: `VITE_API_BASE_URL` = `<Your API Gateway Render URL>`

Make sure to configure Netlify rewrites for React Router by creating a `_redirects` file in the `public/` directory:
```text
/* /index.html 200
```

---

## Continuous Integration (GitHub Actions)

This repository includes a fully automated CI/CD pipeline in `.github/workflows/ci-cd.yml`.
On every push to `main`, the pipeline will:
1. Run `mvn test` across all 10 microservices.
2. Build the executable JARs.
3. Build the Docker images for all services and the React frontend.
