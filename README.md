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
*Note:* Actual production deployment (Cloudflare Pages, Railway/cloud containers, production MySQL provisioning, domain/SSL, and VNPay live credentials) is intentionally deferred to **Phase F**.
