# TIKEY SPA — Security Hardening Checklist

**Target:** Production Operations & Controlled Deployment  
**Repository:** `itkhair05/Spa-Booking-System`  
**Status:** Ready for Controlled Deployment  

---

## 1. Completed Security Controls (Code Level)

| Control ID | Category | Description | Status | Verification Reference |
|---|---|---|---|---|
| **CTRL-01** | Payment Integrity | Prevent VNPay callback from transitioning `REFUNDED` or `CANCELLED` payments back to `PAID`. | Completed | `PaymentService.processVNPayCallback`, `SecurityHardeningIntegrationTest` |
| **CTRL-02** | Payment Integrity | Prevent VNPay IPN from resurrecting `REFUNDED` or `REFUND_PENDING` payments to `PAID`. | Completed | `PaymentService.processVNPayIpn`, `SecurityHardeningIntegrationTest` |
| **CTRL-03** | Abuse Prevention | Rate limit public feedback submissions (`POST /api/v1/public/spas/*/feedback`) to 10 req/min per IP. | Completed | `RateLimitFilter.RULES`, `SecurityHardeningIntegrationTest` |
| **CTRL-04** | Authentication | Enforce startup fail-fast validation for JWT Secret (minimum 256 bits / 32 bytes). | Completed | `JwtUtils.validateSecretKey`, `SecurityHardeningIntegrationTest` |
| **CTRL-05** | Audit & Gateway | Use trusted `ClientIpResolver` for client IP detection during refund processing. | Completed | `RefundController.initiateRefund` |
| **CTRL-06** | Web Defense | Add `X-Frame-Options: DENY` and `Permissions-Policy` to HTTP response headers. | Completed | `SecurityConfig`, `SecurityHardeningIntegrationTest` |
| **CTRL-07** | RBAC Isolation | Server-side validation ensuring `STAFF` cannot view customer records or modify owner accounts. | Completed | `CustomerController`, `StaffController`, existing test suites |
| **CTRL-08** | Multi-Tenant | Strict `TenantContext` isolation across queries for bookings, payments, staff, and services. | Completed | `TenantContextFilter`, `PublicTenantContextFilter`, `BookingService` |
| **CTRL-09** | Anti-Enumeration | Public booking code generated as high-entropy UUID string (`BK-XXXXXXXXXX`), rate-limited at 30 req/min. | Completed | `Booking.onCreate`, `RateLimitFilter` |
| **CTRL-10** | Content Security | Strict MIME-type checking, image magic-byte verification, and UUID filename normalization. | Completed | `FileStorageService`, `AvatarAndServiceImageSecurityTest` |

---

## 2. Production Environment Variables Checklist

Ensure the following variables are configured in Railway environment settings:

| Variable | Description | Security Requirement |
|---|---|---|
| `JWT_SECRET` | Secret key for signing JWT tokens | **Must be $\ge$ 32 bytes** (Recommend 64+ random hex characters: `openssl rand -hex 32`). Never commit default values. |
| `JWT_EXPIRATION_MS` | JWT token validity duration | Set to a reasonable duration (e.g. `86400000` = 24 hours). |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed frontend origins | Explicit origins only: e.g. `https://spa-booking-system-dta.pages.dev,https://tikeyspa.com`. No wildcards (`*`). |
| `SECURITY_RATE_LIMIT_ENABLED` | In-memory rate limiting flag | Set to `true`. |
| `SECURITY_RATE_LIMIT_TRUSTED_PROXIES` | Trusted reverse proxy CIDRs | Configure Railway/Cloudflare proxy IP ranges (e.g. `127.0.0.1,::1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16`). |
| `VNP_HASH_SECRET` | Secret key for VNPay HMAC-SHA512 verification | Keep secret, rotate if previously exposed in non-production environments. |
| `VNP_TMN_CODE` | VNPay Merchant Terminal Code | Official merchant code assigned by VNPay. |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | Set to `prod`. |
| `APP_BOOTSTRAP_OWNER_ENABLED` | Production initial owner bootstrap | Set to `false` after initial admin account creation. |
| `RESEND_API_KEY` | Resend API key for transaction notifications | Provide via Railway dashboard; never commit into code. |

---

## 3. Manual Verification Steps for Deployment

### Step 1: Pre-Deployment Verification
- [x] Run full backend test suite (`mvnw test`): **498 tests passing, 0 failures**.
- [x] Run frontend linter (`npm run lint`): **0 errors**.
- [x] Run frontend production build (`npm run build`): **Build succeeds**.

### Step 2: Deployment to Railway & Cloudflare Pages
1. Push branch `fix/security-audit-and-hardening` and merge to `main` via PR.
2. Verify Railway deployment build logs:
   - Ensure `validateSecretKey()` passes without throwing `IllegalStateException`.
   - Ensure Flyway migrations run cleanly with schema up to date.
   - Confirm active profile is `prod`.

### Step 3: Post-Deployment Smoke Tests
1. **Health Check:**
   - Request `GET https://<backend-domain>/actuator/health` -> Expect `{"status":"UP"}`.
2. **Security Headers Check:**
   - Inspect response headers on any public API route:
     - `X-Frame-Options: DENY`
     - `X-Content-Type-Options: nosniff`
     - `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'`
3. **CORS Verification:**
   - Make an `OPTIONS` request with `Origin: https://spa-booking-system-dta.pages.dev` -> Expect `200 OK` with `Access-Control-Allow-Origin`.
   - Make an `OPTIONS` request with `Origin: https://evil.com` -> Expect `403 Forbidden` or no CORS headers.
4. **Rate Limiting Smoke Test:**
   - Send rapid repeated requests to `POST /api/v1/auth/login` (>10 attempts in 60s) or `POST /api/v1/public/spas/tikey-spa/feedback` (>10 attempts in 60s) -> Verify HTTP `429 Too Many Requests` is returned with `Retry-After` header.
5. **VNPay Payment Flow Smoke Test:**
   - Perform a test booking using test card credentials in VNPay Sandbox.
   - Verify payment transitions to `PAID` and booking status updates to `CONFIRMED`.
   - Re-open the callback URL or simulate a replay -> Verify status remains intact and does not double-charge or cause discrepancies.

---

## 4. Rollback Plan

If unexpected regressions occur in production:
1. Railway allows rolling back to the previous stable deployment release directly from the Railway dashboard in 1 click.
2. The changes in this release do not modify Flyway database schema, so rollbacks do not require database down-migrations.
