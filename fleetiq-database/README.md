# FleetIQ Fleet Management Platform — Database & Kafka Contracts

This project houses the production-ready database schema definition, migrations, maintenance scripts, and event contracts for **FleetIQ**, an enterprise-grade multi-tenant fleet management platform built using PostgreSQL 16, TimescaleDB 2.x, and PostGIS 3.x.

---

## 1. Prerequisites

To run and deploy the migrations, you require:
- **PostgreSQL 16** (Database Engine)
- **TimescaleDB 2.x** (Time-series optimization extension)
- **PostGIS 3.x** (Geospatial extension)
- **pg_partman 4.x/5.x** (Partition manager extension)
- **pg_cron** (In-database job scheduler extension)
- **Flyway CLI 9.x+** (Schema migration runner)

---

## 2. Directory Structure

```
fleetiq-database/
├── er-diagram/
│   └── fleetiq_er_diagram.md       # Full Mermaid ER diagram (~70 tables)
├── kafka-contracts/
│   ├── README.md                   # Kafka topic conventions & compatibility
│   └── *.avsc                      # 8 Confluent Avro schema contracts
├── migrations/
│   ├── V001__extensions_and_schemas.sql
│   ├── V002__tenant_and_auth.sql
│   ├── V003__fleet_structure.sql
│   ├── ...
│   └── V023__seed_data.sql          # 23 Flyway migration steps
├── scripts/
│   ├── backup_recovery.sql          # Base backups, WAL archiving, and failover runbooks
│   └── maintenance_procedures.sql   # Re-indexing, vacuums, and cron logs
└── README.md                       # This setup document
```

---

## 3. Local Setup with Docker

We supply a pre-configured Docker stack bundling PostgreSQL 16 with all required extensions (TimescaleDB, PostGIS, pg_partman, and pg_cron).

### step 1: Write `docker-compose.yml`
Save the following file in the parent project directory:

```yaml
version: '3.8'

services:
  fleetiq-db:
    image: timescale/timescaledb-ha:pg16
    container_name: fleetiq-postgres
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=fleetiq_dev
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgressecret
      - POSTGRES_INITDB_ARGS=--data-checksums
    volumes:
      - fleetiq_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d fleetiq_dev"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  fleetiq_data:
```

### step 2: Spin up the Database Container
```bash
docker-compose up -d
```

---

## 4. Running Schema Migrations (Flyway)

Configure your `flyway.conf` file:
```properties
flyway.url=jdbc:postgresql://localhost:5432/fleetiq_dev
flyway.user=postgres
flyway.password=postgressecret
flyway.schemas=public
flyway.locations=filesystem:./migrations
flyway.connectRetries=10
```

Execute migrations:
```bash
# Verify connection
flyway info

# Run all 23 database migrations
flyway migrate
```

---

## 5. Environment Settings (App Configuration)

The database utilizes Session Variables to enforce Row Level Security (RLS) isolation. Your application connection pool wrapper must execute these setters on every transaction borrow:

```sql
SET app.current_tenant_id = 'b2c3d4e5-1111-4000-8000-000000000001';
SET app.current_user_id = 'e5f6a7b8-2222-4000-8000-000000000001';
SET app.current_user_role = 'FLEET_MANAGER';
```

---

## 6. Retention Policies Summary

High-frequency time-series tables are partitioned into chunks and compressed to save up to 90% space, then dropped automatically according to policies:

| Data Type / Table | Chunk Size | Compression | Hot Retention |
|---|---|---|---|
| **GPS Telemetry (`gps_readings`)** | 4 Hours | Compress after 24h | 90 Days |
| **Fuel Telemetry (`fuel_readings`)** | 4 Hours | Compress after 24h | 90 Days |
| **OBD Diagnostics (`obd_readings`)** | 6 Hours | Compress after 24h | 60 Days |
| **Driving Events (`driving_events`)** | 1 Day | Compress after 48h | 365 Days |
| **EV Battery Logs (`battery_health_readings`)** | 1 Day | Compress after 48h | 365 Days |

---

## 7. Backup & Recovery Overview

- **Continuous Archiving**: WAL logs are switched every 10 minutes and archived to secure object storage (`s3://fleetiq-database-backups/wal/`) to support Point-in-Time Recovery (PITR) to any specific millisecond.
- **Physical Base Backups**: Initiated daily via `fn_initiate_base_backup()` and stored out-of-band.
- **Logical Exporter**: Single tenants can be fully extracted as CSV tables using `fn_export_tenant_data(tenant_id, output_dir)`.

---

## 8. Contributing & Verification Guidelines

When proposing schema modifications:
1. **No direct table alters**: Always write a new sequential Flyway migration (e.g. `V024__add_billing_table.sql`).
2. **Backward-compatible Kafka changes**: Register schema additions withConfluent Registry check tools (`confluent schema-registry compatibility-check`).
3. **Validate RLS**: Verify that every new tenant-scoped table is registered with RLS.
