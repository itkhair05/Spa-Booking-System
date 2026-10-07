# TIKEY SPA — Booking System

[![CI](https://github.com/itkhair05/spa-booking-saas/actions/workflows/ci.yml/badge.svg)](https://github.com/itkhair05/spa-booking-saas/actions/workflows/ci.yml)

Full-stack spa booking & management platform.

## Run with Docker

### Prerequisites
- Docker Engine 24+ & Docker Compose v2+
- Git

### Environment Variables
Copy `.env.example` to `.env` in the project root and provide required secrets for local development:

```bash
cp .env.example .env
```

Ensure `.env` contains your desired passwords, JWT secret, and sandbox payment credentials.

### Build and Start

Build all container images:
```bash
docker compose build
```

Start containers in background:
```bash
docker compose up -d
```

Check running container status:
```bash
docker compose ps
```

### Application URLs
- **Frontend Web UI & Public SPA**: http://localhost:5173
  - Customer Booking & Landing: `http://localhost:5173/`
  - Booking Lookup: `http://localhost:5173/tra-cuu`
  - Management Dashboard: `http://localhost:5173/dashboard`
- **Backend API (Direct)**: http://localhost:8080/api/v1
- **MySQL (Host access)**: `localhost:3307` (database `spabooking_dev`)

### View Logs
```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f mysql
```

### Stop Containers
To stop containers while preserving database and upload volumes:
```bash
docker compose down
```

### Teardown & Remove Volumes
To stop containers and intentionally delete all persistent data (database and uploads):
```bash
docker compose down -v
```

## CI / CD Pipeline & Quality Gate

Automated quality verification and continuous delivery foundations are managed via GitHub Actions:

- **Workflow**: `.github/workflows/ci.yml`

### 1. Continuous Integration (Pull Requests & Main)
Enforces 4 required status checks in parallel on every PR targeting `main` and pushes to `main`:
1. **Git Diff Check**: Enforces clean repository working tree and verifies code formatting/whitespace.
2. **Backend Test**: Runs Java 17 + Spring Boot test suite (343 tests) against a MySQL 8 service container.
3. **Frontend Check**: Runs `npm ci`, ESLint code standards (0 errors, 0 warnings), and Vite production build.
4. **Docker CI**: Validates Docker Compose configuration, builds backend & frontend images, and verifies startup healthchecks.

All four checks must **PASS** before Pull Requests are eligible for merging into `main`.

### 2. CD Foundation (Post-Merge on Main)
Once all 4 CI checks pass on the protected `main` branch, the `CD Foundation` job automatically produces release-ready Docker artifacts:
- Builds immutable production images tagged with the Git commit SHA:
  - `tikey-spa-backend:<commit-sha>`
  - `tikey-spa-frontend:<commit-sha>`
- Injects standard OCI container labels (revision, title, created timestamp).
- Verifies image integrity and inspects layer sizes without publishing to external registries.

### 3. Production Deployment (Scope Boundary)
*Note:* Actual production deployment (Cloudflare Pages, Railway/cloud containers, production MySQL provisioning, domain/SSL, and VNPay live credentials) is executed in **Phase F**.

## Spring Boot Profiles & Configuration

The backend supports two distinct runtime configuration profiles:

### 1. Development Profile (`SPRING_PROFILES_ACTIVE=dev`)
- Used by default in local Docker Compose (`docker-compose.yml`) and local development.
- **DevDataSeeder**: Active (`@Profile("dev")`). Automatically seeds demo owner account (`owner@demo.local`), demo staff, and initial catalog using passwords provided in `DEV_OWNER_PASSWORD` and `DEV_STAFF_PASSWORD`.
- **SQL Logging**: Enabled (`spring.jpa.show-sql=true`, `format_sql=true`) for inspection.
- **Payment & CORS**: Uses VNPay Sandbox defaults and localhost origins.

### 2. Production Profile (`SPRING_PROFILES_ACTIVE=prod`)
- Activated with `SPRING_PROFILES_ACTIVE=prod`.
- **Configuration Source**: [application-prod.properties](file:///f:/spa-booking-system/backend/src/main/resources/application-prod.properties).
- **DevDataSeeder**: **Strictly disabled**. No demo users, staff, or placeholder data are created.
- **Database & Schema**: Managed authoritatively via Flyway migrations with `spring.jpa.hibernate.ddl-auto=validate`. No auto-generation or alteration of database tables by Hibernate.
- **Connection Pool**: HikariCP connection pool configured (`maximum-pool-size=10`, `minimum-idle=5`, timeouts).
- **Graceful Shutdown**: Enabled (`server.shutdown=graceful`) with 20s shutdown phase timeout.
- **SQL Logging**: **Disabled** (`show-sql=false`, `format_sql=false`) to protect sensitive data and prevent log bloat.
- **Fast-fail Validation**: Production requires `SPRING_DATASOURCE_URL`, `DB_PASSWORD`, `JWT_SECRET`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL`, and `CORS_ALLOWED_ORIGINS`. If any required secret is missing, application startup fails fast without fallback to sandbox or localhost.
- **Security**: Real secrets must be injected through cloud container environment variables. Never commit `.env` or production credentials to Git.

## Production Security & Network Hardening

### 1. MySQL Network Isolation
- **Development**: MySQL port defaults to `127.0.0.1:3307:3306` (`MYSQL_BIND_IP=127.0.0.1`), ensuring it is reachable locally via `localhost:3307` while preventing exposure to the public internet (`0.0.0.0`).
- **Production**: Run with the production compose override:
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
  ```
  `docker-compose.prod.yml` completely strips the host port mapping (`ports: !reset []`). MySQL communicates exclusively with the backend via the private Docker bridge network (`spa-booking-network`).

### 2. Reverse Proxy & Real Client IP Resolution
- **Rate Limiting**: Rate limiter resolves real client IPs via `ClientIpResolver`.
- **Anti-Spoofing**: Forwarding headers (`CF-Connecting-IP`, `X-Real-IP`, `X-Forwarded-For`) are trusted **ONLY** when the immediate connection originates from a recognized trusted proxy (configured via `SECURITY_TRUSTED_PROXIES`, defaulting to loopback `127.0.0.1`, `::1` and RFC 1918 private subnets `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
- Direct untrusted connections attempting to forge `CF-Connecting-IP` or `X-Forwarded-For` are ignored, and the rate limiter keys by their actual socket connection IP.
- **Cloudflare Edge**: When deployed behind Cloudflare, Nginx forwards `CF-Connecting-IP`. If Nginx `real_ip` module is configured in production, official Cloudflare IP CIDR ranges must be configured in Nginx rather than using broad wildcard subnets.
