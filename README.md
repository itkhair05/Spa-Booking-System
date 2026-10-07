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

## Continuous Integration & Quality Gate

Automated quality verification is enforced via GitHub Actions on every push to `main` and all Pull Requests targeting `main`:

- **Workflow**: `.github/workflows/ci.yml`
- **Required Checks**:
  1. **Git Diff Check**: Enforces clean repository working tree and verifies code formatting/whitespace.
  2. **Backend Test**: Runs Java 17 + Spring Boot test suite against a MySQL 8 service container.
  3. **Frontend Check**: Runs `npm ci`, ESLint code standards (0 errors, 0 warnings), and Vite production build.
  4. **Docker CI**: Validates Docker Compose configuration, builds backend & frontend production images, and executes startup healthchecks.

All four checks must **PASS** before Pull Requests are eligible for merging into `main`.
