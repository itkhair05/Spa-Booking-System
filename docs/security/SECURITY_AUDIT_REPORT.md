# TIKEY SPA — Security Audit Report

**Date:** 2026-10-09  
**Application:** TIKEY SPA (Spa Booking System)  
**Target Environment:** Production (Railway Backend/MySQL + Cloudflare Pages Frontend)  
**Branch:** `fix/security-audit-and-hardening`  
**Auditor:** Senior Application Security Engineer & Java Spring Boot Security Specialist  

---

## 1. Executive Summary

A comprehensive application security audit and hardening assessment was conducted on the TIKEY SPA codebase. The application is an existing production spa booking platform designed for future multi-tenant Micro-SaaS operation.

The assessment covered eight core operational and architectural domains (B1 through B8):
1. Authentication and JWT Lifecycle
2. Authorization and Role-Based Access Control (RBAC)
3. Multi-Tenant Isolation and Data Scoping
4. Booking and Customer Data Protection
5. Payment Gateway Security (VNPay Integration & Idempotency)
6. Input Validation and Injection Defense (SQLi, XSS, File Uploads)
7. Web Security Configuration (CORS, CSRF, Rate Limiting, Security Headers)
8. Secrets, Database Configuration, and Operational Security

All confirmed vulnerabilities were remediated using minimal, maintainable changes that preserve existing business logic, database migrations, and API contracts. Fixes were verified with dedicated automated integration tests and a full backend test run (498 tests passing, 0 failures), along with frontend lint and production build checks.

---

## 2. Audit Scope & Methodology

### 2.1 Technology Stack Under Review
- **Backend:** Java 17, Spring Boot 4.1.1, Spring Security 6.x, JJWT 0.12.6, Spring Data JPA / Hibernate 7.4.5.
- **Database:** MySQL 8.x with Flyway migration management (14 migrations).
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS.
- **Infrastructure:** Railway (Spring Boot API + MySQL database), Cloudflare Pages (SPA Frontend).

### 2.2 Methodology
The assessment followed OWASP ASVS (Application Security Verification Standard) guidelines, Spring Boot security best practices, and payment gateway security specifications:
1. **Reconnaissance:** Codebase analysis of Spring Security filters, JWT authentication, tenant context filters, route authorizations, payment webhook handlers, upload handlers, and email dispatchers.
2. **Threat Modeling & Logic Flaw Analysis:** Verification of race conditions, callback replays, horizontal and vertical privilege escalation (IDOR), secret entropy, rate limiting gaps, and tenant boundary enforcement.
3. **Remediation:** Surgical code-level fixes with zero disruption to active business features.
4. **Automated Verification:** Implementation of unit and integration tests asserting both positive and negative security invariants.

---

## 3. Detailed Security Findings & Remediations

### Finding SEC-01 [HIGH] — VNPay Callback & IPN State Machine Reversion
- **Affected Components:**
  - `com.example.spabooking.payment.service.PaymentService.processVNPayCallback()`
  - `com.example.spabooking.payment.service.PaymentService.processVNPayIpn()`
- **Vulnerability Explanation:**
  - In `processVNPayCallback()`, the code previously only checked if the local payment was `PAID`. If a payment had already been transitioned to `REFUNDED`, `REFUND_PENDING`, `CANCELLED`, or `FAILED`, a delayed browser callback or replayed GET request with `vnp_ResponseCode=00` would overwrite the payment status to `PAID`, record a new `paidAt` timestamp, and execute `confirmBookingOnPaymentSuccess()`.
  - In `processVNPayIpn()`, the code checked `CANCELLED` and `FAILED`, but did not check `REFUNDED` or `REFUND_PENDING`. If a payment was already refunded by the spa owner, a replayed IPN notification from VNPay could resurrect the transaction to `PAID`.
- **Attack / Failure Scenario:**
  - A customer cancels an appointment or is issued a refund. The customer re-opens an old browser tab containing the VNPay callback URL (`/payments/vnpay-callback?vnp_ResponseCode=00&...`), causing the backend to mark the refunded payment as `PAID` and confirm the booking again without money being collected.
- **Remediation Applied:**
  - In `processVNPayCallback()`, added explicit state guards: payments with status `REFUNDED` or `REFUND_PENDING` return `VNPayCallbackResult(false, ...)` without mutating the entity; payments with status `CANCELLED` or `FAILED` return `VNPayCallbackResult(false, ...)` without mutating the entity.
  - In `processVNPayIpn()`, added `REFUNDED` and `REFUND_PENDING` to terminal state checks, returning VNPay response code `"02"` ("Order already confirmed/updated") to instruct VNPay not to re-attempt delivery, without mutating payment or booking state.
- **Status:** **FIXED** (Verified by `SecurityHardeningIntegrationTest`).

---

### Finding SEC-02 [MEDIUM] — Public Customer Feedback Endpoint Lacked Rate Limiting
- **Affected Components:**
  - `com.example.spabooking.common.ratelimit.RateLimitFilter`
  - `com.example.spabooking.publicapi.controller.PublicController.submitFeedback()`
- **Vulnerability Explanation:**
  - The public endpoint `POST /api/v1/public/spas/{slug}/feedback` is unauthenticated to allow visitors to submit feedback and inquiries. However, `RateLimitFilter.RULES` did not contain an entry for feedback submission.
- **Attack / Failure Scenario:**
  - An automated bot or malicious user could spam hundreds of feedback submissions per second, filling the database table `feedback`, polluting the spa owner's feedback inbox, and consuming server resources.
- **Remediation Applied:**
  - Added rate limiting rule `new Rule("POST", "/api/v1/public/spas/*/feedback", 10, 60)` to `RateLimitFilter.RULES`, capping requests to 10 per minute per client IP.
- **Status:** **FIXED** (Verified by `SecurityHardeningIntegrationTest`).

---

### Finding SEC-03 [MEDIUM] — Missing Startup Validation for JWT Secret Key Entropy
- **Affected Components:**
  - `com.example.spabooking.auth.security.JwtUtils`
- **Vulnerability Explanation:**
  - `jwtSecret` was injected via `@Value("${jwt.secret}")` without startup validation. If `JWT_SECRET` was omitted, empty, or shorter than 256 bits (32 bytes), the application would start up successfully, but all subsequent authentication attempts would fail at runtime with `WeakKeyException` or `IllegalArgumentException`.
- **Attack / Failure Scenario:**
  - Insecure production deployments or misconfigured environment variables could launch with weak keys or degrade authentication reliability.
- **Remediation Applied:**
  - Added `@PostConstruct public void validateSecretKey()` to `JwtUtils` that validates `jwtSecret` is non-null, non-empty, and possesses at least 32 UTF-8 bytes (256 bits). It fails application startup immediately with an explicit, secure configuration error if the requirement is not met.
- **Status:** **FIXED** (Verified by unit test in `SecurityHardeningIntegrationTest`).

---

### Finding SEC-04 [MEDIUM] — Inconsistent Client IP Resolution in Refund Controller
- **Affected Components:**
  - `com.example.spabooking.payment.controller.RefundController.initiateRefund()`
- **Vulnerability Explanation:**
  - While `PaymentController` and `RateLimitFilter` used `ClientIpResolver` (which securely evaluates `X-Forwarded-For` and `CF-Connecting-IP` against configured trusted proxy CIDRs), `RefundController` invoked `httpServletRequest.getRemoteAddr()` directly.
  - When deployed behind Railway and Cloudflare reverse proxies, `getRemoteAddr()` resolves to the internal proxy IP (e.g. `127.0.0.1` or private subnet) rather than the actual client IP, distorting refund audit records and VNPay QueryDR/refund API metadata.
- **Remediation Applied:**
  - Injected `ClientIpResolver` into `RefundController` (with backward-compatible constructor overload) and updated `initiateRefund` to resolve `clientIp` via `clientIpResolver.resolveClientIp(httpServletRequest)`.
- **Status:** **FIXED** (Verified by full test suite compilation and runtime test pass).

---

### Finding SEC-05 [LOW] — Hardening HTTP Defense Headers
- **Affected Components:**
  - `com.example.spabooking.auth.config.SecurityConfig`
- **Vulnerability Explanation:**
  - Although Content-Security-Policy (CSP), HSTS, and Referrer-Policy headers were configured, `X-Frame-Options` and `Permissions-Policy` headers were not explicitly enforced in `SecurityConfig`.
- **Remediation Applied:**
  - Configured `.frameOptions(frame -> frame.deny())` and `.permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=()"))` in `SecurityConfig`.
- **Status:** **FIXED** (Verified by `SecurityHardeningIntegrationTest`).

---

## 4. Assessment of Non-Vulnerable Controls

The following controls were audited and confirmed to already be properly designed:

1. **Role-Based Access Control (RBAC):**
   - OWNER endpoints are strictly guarded by `@PreAuthorize("hasRole('OWNER')")`.
   - STAFF accounts cannot access customer management, staff account provisioning, service price updates, or another staff member's bookings.
2. **Multi-Tenant Isolation:**
   - Multi-tenant boundary is enforced via `TenantContextFilter`, `PublicTenantContextFilter`, and `TenantContext.requireTenantId()`.
   - All repository queries filter by `tenantId`.
   - Entity lookups (services, bookings, staff, customers, payments, refunds) verify tenant ownership before returning records.
3. **IDOR on Booking Codes:**
   - Public booking lookup relies on `bookingCode`, which is generated via `UUID.randomUUID()` (e.g. `BK-XXXXXXXXXX`), preventing sequential enumeration.
   - Public lookup endpoint is rate-limited (30 requests/minute).
4. **File Upload Security:**
   - `FileStorageService` enforces strict MIME-type whitelisting (`image/jpeg`, `image/png`, `image/webp`), size limits (5MB max), magic byte validation, and path traversal prevention (normalizing filenames to UUIDs).
5. **SQL & JPQL Injection:**
   - All database queries utilize Spring Data JPA parameter binding or JPA Criteria API. No raw string concatenation in SQL/JPQL queries was found.
6. **Cross-Site Scripting (XSS):**
   - React frontend uses standard JSX text interpolation (auto-escaped).
   - Backend API responds with strict `application/json` and CSP `default-src 'none'`.
7. **Production Seeders:**
   - `DevDataSeeder` is isolated under `@Profile("dev")`.
   - `ProductionOwnerBootstrapSeeder` is guarded under `@Profile("prod")` and disabled by default (`app.bootstrap.owner.enabled=false`).

---

## 5. Automated Verification Results

### 5.1 New Security Test Suite (`SecurityHardeningIntegrationTest.java`)
- `testVNPayCallbackCannotRevertRefundedPaymentToPaid` — **PASSED**
- `testVNPayCallbackCannotRevertCancelledPaymentToPaid` — **PASSED**
- `testVNPayIpnCannotRevertRefundedPaymentToPaid` — **PASSED**
- `testPublicFeedbackRateLimiting` — **PASSED** (10 allowed, 11th returns 429)
- `testSecurityHeadersPresent` — **PASSED** (CSP, Frame-Options, Permissions-Policy, HSTS)
- `testTamperedJwtSignatureRejected` — **PASSED** (HTTP 401)
- `testJwtUtilsFailsFastOnWeakSecret` — **PASSED** (Throws `IllegalStateException` on keys < 32 bytes)
- `testStaffCannotAccessCustomerManagement` — **PASSED** (HTTP 403)
- `testCrossTenantBookingAccessBlocked` — **PASSED** (HTTP 404 / IDOR prevented)

### 5.2 Full Backend Test Suite
```
[INFO] Results:
[INFO] Tests run: 498, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS (Time: 01:52 min)
```

### 5.3 Frontend Verification
```
> frontend@0.0.0 lint
> eslint .
(Exit code: 0)

> frontend@0.0.0 build
> tsc -b && vite build
✓ built in 646ms (Exit code: 0)
```

---

## 6. Residual Risks & Operational Recommendations

1. **Production Secret Rotation:** Ensure `JWT_SECRET` in Railway has at least 64 random characters (e.g. generated via `openssl rand -hex 32`).
2. **Reverse Proxy Configuration:** Verify that Railway passes `X-Forwarded-For` and Cloudflare provides `CF-Connecting-IP`, and confirm that `SECURITY_RATE_LIMIT_TRUSTED_PROXIES` includes the upstream proxy CIDRs.
3. **Database Credentials:** Ensure the MySQL database user in production does not possess `SUPER` or `GRANT OPTION` privileges and adheres to least-privilege principles.
