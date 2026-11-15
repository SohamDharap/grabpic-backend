# GrabPic Backend

Spring Boot backend for GrabPic — event photo management with face recognition search.

## Stack

- Spring Boot 4 / Java 21
- PostgreSQL + pgvector
- Spring Security (JWT + OTP)
- Docker & Docker Compose

## Quick Start

```bash
# Build
mvn clean package -DskipTests

# Run with Docker Compose
docker compose up -d
```

## Monitoring & Health Endpoints

The backend exposes Spring Boot Actuator endpoints for production monitoring on AWS EC2, Docker, and load balancers.

### Public endpoints (no authentication)

These endpoints are intentionally public for load balancers, Docker health checks, and uptime monitors:

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator/health` | Aggregated health: app status, DB connectivity, DB version, JVM memory, disk space, uptime, timestamp |
| `GET /actuator/info` | Build metadata: application name, version, build time, Java version |
| `GET /api/system/status` | Detailed system status JSON for dashboards and ops tooling |

### Protected endpoints (JWT required)

| Endpoint | Purpose |
|----------|---------|
| `GET /actuator` | Actuator discovery — lists available monitoring endpoints |
| `GET /actuator/metrics` | Application metrics (JVM, HTTP, datasource, etc.) |
| `GET /actuator/env` | Environment properties (sensitive values masked) |
| `GET /actuator/prometheus` | Prometheus scrape endpoint for Grafana/Alertmanager |

### Examples

**Health check (Docker / ALB / uptime monitor):**

```bash
curl http://localhost:8080/actuator/health
```

Example response:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "databaseVersion": {
      "status": "UP",
      "details": { "version": "PostgreSQL 16.x ..." }
    },
    "diskSpace": { "status": "UP", "details": { "total": "...", "free": "..." } },
    "jvmMemory": { "status": "UP", "details": { "used": "...", "max": "..." } },
    "uptime": { "status": "UP", "details": { "formatted": "1h 2m 3s" } },
    "timestamp": { "status": "UP", "details": { "timestamp": "2026-06-14T12:00:00Z" } }
  }
}
```

**Build information:**

```bash
curl http://localhost:8080/actuator/info
```

**Detailed system status:**

```bash
curl http://localhost:8080/api/system/status
```

Example response:

```json
{
  "application": "GrabPic Backend",
  "status": "UP",
  "version": "0.0.1-SNAPSHOT",
  "javaVersion": "21.0.x",
  "springBootVersion": "4.0.3",
  "uptime": "2h 15m 30s",
  "database": {
    "status": "UP",
    "name": "grabpic_db",
    "version": "PostgreSQL 16.x ...",
    "host": "postgres"
  },
  "system": {
    "cpuCores": 4,
    "freeMemory": "512.00 MB",
    "totalMemory": "256.00 MB",
    "maxMemory": "2.00 GB"
  },
  "disk": {
    "totalSpace": "50.00 GB",
    "freeSpace": "30.00 GB"
  },
  "time": "2026-06-14T12:00:00Z"
}
```

**Prometheus metrics (requires JWT):**

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/prometheus
```

### Docker health check

The backend container health check calls `/actuator/health` every 30 seconds:

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 60s
```

## API Documentation

See [postman/GrabPic-API-Collection.postman_collection.json](postman/GrabPic-API-Collection.postman_collection.json) for the full REST API collection.

## Environment Variables

| Variable | Description |
|----------|-------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |
| `APP_JWT_SECRET` | JWT signing secret |
| `APP_UPLOAD_DIR` | File upload directory (default: `/storage`) |
