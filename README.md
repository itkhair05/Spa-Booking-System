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

### 3. Production Initial Tenant & OWNER Bootstrap
- **One-time Initialization**: In an empty production database, `DevDataSeeder` is inactive. The initial Tenant and OWNER account are created via `ProductionOwnerBootstrapSeeder`.
- **Explicit Activation**: Disabled by default (`PRODUCTION_BOOTSTRAP_ENABLED=false`). Requires:
  ```bash
  PRODUCTION_BOOTSTRAP_ENABLED=true
  BOOTSTRAP_TENANT_NAME="TIKEY SPA"
  BOOTSTRAP_TENANT_SLUG="tikey-spa"
  BOOTSTRAP_OWNER_USERNAME="owner@tikeyspa.com"
  BOOTSTRAP_OWNER_PASSWORD="<STRONG_PASSWORD_MIN_8_CHARS>"
  ```
  Optional business fields: `BOOTSTRAP_TENANT_PHONE`, `BOOTSTRAP_TENANT_EMAIL`, `BOOTSTRAP_TENANT_ADDRESS`, `BOOTSTRAP_TENANT_TIMEZONE`.
- **Operational Workflow**:
  1. Set the bootstrap environment variables on the production container/service.
  2. Start the application once under `SPRING_PROFILES_ACTIVE=prod`.
  3. Verify OWNER authentication via `/api/v1/auth/login`.
  4. Disable `PRODUCTION_BOOTSTRAP_ENABLED=false` (or remove the flag) and remove `BOOTSTRAP_OWNER_PASSWORD` from environment settings.
- **Idempotency & Safety**: If the target Tenant slug or OWNER username already exists, bootstrap safely skips without modifying data or resetting passwords. Passwords are never logged and are hashed using BCrypt.

## VNPay IPN & QueryDR Reconciliation (Phase F.4)

### 1. Server-to-Server VNPay IPN
- **Endpoint**: `GET` / `POST` `/api/v1/payments/vnpay-ipn` (and `/api/v1/public/spas/{slug}/payments/vnpay-ipn`).
- **Security**: Publicly accessible via exact URL match in Spring Security (`permitAll()`). Timing-attack resistant HMAC-SHA512 checksum validation (`MessageDigest.isEqual`).
- **Server-Authoritative Validation**: Expected amount is strictly validated against the server-side payment entity amount before updating status.
- **Idempotency & State Safety**:
  - `PENDING` -> `PAID` (on `vnp_ResponseCode=00`) returns `{"RspCode":"00","Message":"Confirm Success"}`.
  - Repeated/duplicate notifications on terminal states (`PAID`, `CANCELLED`, `FAILED`) return `{"RspCode":"02","Message":"Order already confirmed"}` without duplicate business side effects.
  - Terminal `PAID` payments are never downgraded to `FAILED` or `CANCELLED`.
  - Amount mismatch returns `{"RspCode":"04","Message":"Invalid Amount"}`.
  - Invalid signature returns `{"RspCode":"97","Message":"Invalid Checksum"}`.
  - Unknown payment returns `{"RspCode":"01","Message":"Order not found"}`.

### 2. VNPay QueryDR Reconciliation
- **Endpoint**: `POST /api/v1/payments/{paymentId}/reconcile`
- **Authorization**: Protected, `ROLE_OWNER` only. Enforces tenant boundary isolation.
- **Reconciliation Policy**:
  - `PENDING` + Remote `00` -> Reconciled to `PAID`.
  - `PENDING` + Remote `02`/`09` -> Reconciled to `FAILED`.
  - `PAID` + Remote `00` -> Idempotent no-op (`reconciled = true`).
  - `PAID` + Remote != `00` -> Flagged as discrepancy (`discrepancy = true`), payment is **not** downgraded.
  - `FAILED`/`CANCELLED` + Remote `00` -> Flagged as discrepancy (`discrepancy = true`), requires manual review.
- **Configuration**:
  - Development / Sandbox: `VNPAY_QUERYDR_URL=https://sandbox.vnpayment.vn/merchant_webapi/api/transaction`
  - Production: `VNPAY_QUERYDR_URL=https://vnpayment.vn/merchant_webapi/api/transaction`

## Health & Observability (Phase F.5)

### 1. Actuator Endpoints & Probes
Spring Boot Actuator is configured with minimal, production-safe settings:
- **Aggregated Health Endpoint**: `GET /actuator/health`
  - Returns `{"status":"UP"}` with HTTP 200 when all core components (including database) are healthy.
  - Returns `{"status":"DOWN"}` with HTTP 503 when the database or critical components are unavailable.
- **Liveness Probe**: `GET /actuator/health/liveness`
  - Verifies the process is alive and internal application state is valid (`livenessState`).
- **Readiness Probe**: `GET /actuator/health/readiness`
  - Verifies the application is ready to accept traffic (`readinessState` and `db`).
- **Security & Detail Masking**:
  - `management.endpoint.health.show-details=never`: Responses omit internal database credentials, connection strings, disk info, and stack traces.
  - `management.endpoints.web.exposure.include=health`: Only the `health` endpoint is exposed via web/HTTP. Sensitive Actuator endpoints (`/actuator/env`, `/actuator/beans`, `/actuator/configprops`, `/actuator/heapdump`, etc.) are completely disabled from web exposure.
  - In `SecurityConfig`, only `/actuator/health` and `/actuator/health/**` are accessible unauthenticated for container orchestrators; root `/actuator` and any other management paths require authentication.

### 2. Docker Healthcheck Integration
The backend service in `docker-compose.yml` uses the Actuator health endpoint:
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 6
  start_period: 30s
```
This ensures container orchestrators (Docker Compose, ECS, Kubernetes) accurately track runtime health without relying on arbitrary file probes.

## Cloudflare + Domain + HTTPS (Phase F.6 & F.6.1)

### 1. Production Architecture & Topologies
The production architecture separates static frontend delivery, containerized API execution, and managed relational persistence:

```text
                         Internet
                            │
                            ▼
                       Cloudflare
                      DNS / Proxy
                     Edge HTTPS/CDN
                       /         \
                      /           \
                     ▼             ▼
          Cloudflare Pages      Railway
             React/Vite       Spring Boot API
            example.com       api.example.com
                                      │
                                      │ TLS (sslMode=REQUIRED)
                                      ▼
                                 Aiven MySQL
```

In contrast, local development runs entirely on the developer host:

```text
Browser
   │
   ▼
React/Vite (localhost:5173)
   │
   ▼
Spring Boot (localhost:8080)
   │
   ▼
Docker MySQL (localhost:3307)
```

- **Frontend (Cloudflare Pages)**: Built React/Vite SPA hosted statically on Cloudflare Pages on the root domain (e.g. `https://example.com`, `https://www.example.com`). All client-side SPA routes fallback to `/index.html` via `_redirects`.
- **Backend (Railway)**: Spring Boot API deployed as the sole application service on Railway, accessible via a custom API subdomain (e.g. `https://api.example.com`).
- **Database (Aiven MySQL)**: External managed MySQL database provided by Aiven. The Railway backend connects directly to Aiven MySQL over TLS (`sslMode=REQUIRED`). Aiven MySQL is NOT on a Railway private network, NOT deployed as a Railway service, and is never exposed directly to the public web or Cloudflare DNS.
- **Single-Spa Product**: Single tenant experience for TIKEY SPA (`tikey-spa`); no multi-tenant domain routing, tenant switching, or SaaS custom domain registration required.

### 2. Deployment Responsibilities

| Component | Provider | Responsibilities |
| :--- | :--- | :--- |
| **Edge & DNS** | **Cloudflare** | Authoritative DNS resolution, edge HTTPS termination, DDoS protection, edge caching/CDN, and client IP header forwarding (`CF-Connecting-IP`). |
| **Frontend** | **Cloudflare Pages** | Static hosting of React/Vite SPA build artifacts (`dist/`), edge asset distribution, and client-side SPA route rewrites via `public/_redirects`. |
| **Backend API** | **Railway** | Spring Boot runtime execution, custom domain management (`api.example.com`), automatic Let's Encrypt TLS renewal, and secure environment variable injection. |
| **Database** | **Aiven** | Managed MySQL storage, automated backups, high availability according to selected service plan, and mandatory TLS encrypted connections. |

### 3. DNS & Domain Configuration
Use placeholders such as `example.com` and `api.example.com` when configuring environments:
1. **Frontend (Cloudflare Pages)**:
   - Connect the repository/build output to Cloudflare Pages.
   - In Cloudflare Pages custom domains: associate `example.com` (and `www.example.com`).
   - Cloudflare automatically routes traffic to the static Pages deployment.
2. **Backend API (Railway)**:
   - In Railway dashboard: Service Settings -> Networking -> Custom Domain -> add `api.example.com`.
   - Railway generates custom domain DNS records (CNAME and TXT verification record).
   - In Cloudflare DNS: add the CNAME pointing `api` to the Railway DNS target.
3. **Origin Encryption Mode**:
   - In Cloudflare SSL/TLS dashboard: configure encryption mode to **Full** (or Full strict once Railway origin certificates are provisioned).
   - **Do NOT use Flexible SSL**: Flexible terminates HTTPS at Cloudflare but sends unencrypted HTTP to Railway, breaking secure cookies, HSTS, and credentials. Railway automatically provisions Let's Encrypt certificates for the custom API domain.

### 4. Production Database Configuration (Aiven MySQL over TLS)
- The production datasource configuration is completely environment-driven via `${SPRING_DATASOURCE_URL}`, `${SPRING_DATASOURCE_USERNAME}`, and `${DB_PASSWORD}` without any Docker service hostname (`mysql`) or localhost fallbacks.
- Connections to Aiven MySQL **must enforce TLS**:
  ```text
  SPRING_DATASOURCE_URL=jdbc:mysql://YOUR_AIVEN_HOST:YOUR_AIVEN_PORT/defaultdb?useUnicode=true&characterEncoding=utf-8&sslMode=REQUIRED&serverTimezone=Asia/Ho_Chi_Minh
  ```
- Plaintext database connections (`useSSL=false`) are strictly forbidden in production. Certificate verification is preserved.
- Real Aiven hostnames, ports, usernames, and passwords are never committed to Git and are configured exclusively in Railway environment variables during F.7 deployment.

### 5. Production Environment Variables
| Variable | Component | Example Value | Description |
| :--- | :--- | :--- | :--- |
| `CORS_ALLOWED_ORIGINS` | Backend (Railway) | `https://example.com,https://www.example.com` | Restricts CORS to the Cloudflare Pages domain; wildcards (`*`) are strictly rejected when credentials are enabled. |
| `VITE_API_BASE_URL` | Frontend (Cloudflare Pages) | `https://api.example.com/api/v1` | Target API domain for browser requests; normalized automatically by the frontend API client. |
| `SPRING_DATASOURCE_URL` | Backend (Railway) | `jdbc:mysql://YOUR_AIVEN_HOST:YOUR_AIVEN_PORT/defaultdb?...&sslMode=REQUIRED` | Managed database JDBC URL requiring TLS encryption to Aiven MySQL. |
| `SPRING_DATASOURCE_USERNAME`| Backend (Railway) | `avnadmin` | Aiven MySQL database username provided by Railway environment variable. |
| `DB_PASSWORD` | Backend (Railway) | `CHANGE_ME_AIVEN_DB_PASSWORD` | Aiven MySQL database password provided by Railway environment variable. |
| `VNPAY_RETURN_URL` | Backend (Railway) | `https://example.com/dat-lich/callback` | Browser redirect URL after VNPay payment completion (points to Cloudflare Pages). |
| `VNPAY_IPN_URL` | Backend (Railway) | `https://api.example.com/api/v1/payments/vnpay-ipn` | Server-to-server webhook callback URL for VNPay IPN notifications (points to Railway API). |
| `SECURITY_TRUSTED_PROXIES` | Backend (Railway) | `127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16` | Trusted reverse proxy subnets for anti-spoofing and real client IP resolution (`CF-Connecting-IP`). |
| `SERVER_FORWARD_HEADERS_STRATEGY` | Backend (Railway) | `framework` | Activates Spring Framework `ForwardedHeaderFilter` to recognize edge HTTPS headers (`X-Forwarded-Proto`). |
