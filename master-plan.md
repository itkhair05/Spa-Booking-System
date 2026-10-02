# Spa Booking Micro-SaaS — Master Plan

> **Document Status:** Living Specification  
> **Version:** 1.0  
> **Project Type:** Micro-SaaS  
> **Target Market:** Small Spa / Salon businesses in Vietnam  
> **Primary Goal:** Build a production-minded SaaS while learning and strengthening Java + Spring Boot.

---

# 1. Product Vision

## 1.1 Overview

**Spa Booking Micro-SaaS** is a lightweight appointment management platform designed for small Spa and Salon businesses in Vietnam, typically with **1–5 staff members**.

The product focuses on solving the core operational problems of small businesses:

- Managing appointments.
- Preventing double bookings.
- Managing services and staff.
- Managing customer information.
- Providing an online booking page for customers.
- Giving the owner a simple dashboard to understand the daily schedule.
- Sending appointment reminders.

The product should be:

- Simple.
- Fast.
- Mobile-friendly.
- Easy for non-technical business owners.
- Affordable in concept.
- Multi-tenant from the beginning.
- Designed as a real SaaS rather than a demo CRUD application.

---

# 2. Product Goals

## 2.1 Primary Goals

The MVP must allow a small Spa/Salon to:

1. Create an account/business.
2. Log in securely.
3. Manage services.
4. Manage staff.
5. Manage customers.
6. View appointments.
7. Create appointments manually.
8. Update appointment status.
9. Prevent conflicting appointments.
10. Provide a public booking page.
11. Allow customers to book without creating an account.
12. Send appointment reminders.
13. Keep each business's data isolated from other businesses.

---

# 2.2 Secondary Goals

After the MVP is stable, the system may support:

- Business settings.
- Working hours.
- Staff schedules.
- Holiday / day-off management.
- Customer history.
- Basic analytics.
- Exporting data.
- Multiple branches.
- Subscription plans.
- Usage limits.
- Payment integration.
- More notification channels.

These are **not part of the initial MVP unless explicitly requested**.

---

# 3. Target Users

## 3.1 Business Owner

Typical characteristics:

- Owns a small Spa or Salon.
- Has approximately 1–5 staff.
- May not be technically experienced.
- Needs a simple way to manage appointments.
- Often manages the business from a phone.

Primary needs:

- See today's appointments quickly.
- Add/edit/cancel appointments.
- Manage staff.
- Manage services.
- See customer information.
- Share an online booking link.

---

## 3.2 Staff

Staff members need to:

- View their assigned appointments.
- See customer information.
- See service information.
- Update appointment status where permitted.

Staff should have fewer permissions than the owner.

---

## 3.3 Customer

Customers do not need an account for the MVP.

They should be able to:

1. Open the business booking page.
2. Select a service.
3. Select a staff member or choose "Any available staff".
4. Select a date.
5. Select an available time slot.
6. Enter contact information.
7. Submit the booking.
8. Receive a confirmation.

---

# 4. Core Product Flow

## 4.1 Business Setup

```text
Create Account
      ↓
Create Business / Tenant
      ↓
Create First Service
      ↓
Create Staff
      ↓
Business Ready
```

---

## 4.2 Owner Booking Flow

```text
Dashboard
    ↓
Calendar
    ↓
Select Date
    ↓
Create Booking
    ↓
Select Customer
    ↓
Select Service
    ↓
Select Staff
    ↓
Select Time
    ↓
Validate Availability
    ↓
Create Booking
```

---

## 4.3 Public Booking Flow

```text
Public Booking Page
        ↓
Select Service
        ↓
Select Staff / Any Staff
        ↓
Select Date
        ↓
Display Available Time Slots
        ↓
Customer Information
        ↓
Review Booking
        ↓
Create Booking
        ↓
Confirmation
```

---

# 5. MVP Scope

## 5.1 Included in MVP

### Authentication

- Login.
- Logout.
- JWT authentication.
- Password hashing.
- Role-based authorization.

### Tenant Management

- Business/Tenant creation.
- Tenant identification.
- Tenant data isolation.

### Services

- Create service.
- Update service.
- Activate/deactivate service.
- View service list.

Service fields:

- Name.
- Description.
- Duration.
- Price.
- Active status.

### Staff

- Create staff.
- Update staff.
- Activate/deactivate staff.
- View staff list.

### Customers

- Create customer.
- Update customer.
- View customer.
- View booking history.

### Bookings

- Create booking.
- View bookings.
- Update booking.
- Cancel booking.
- Change booking status.
- Filter bookings by date/staff/status.
- Prevent conflicting bookings.

### Public Booking

- Public business page.
- Public service list.
- Staff selection.
- Available time slots.
- Customer booking without login.

### Notifications

- Appointment reminder.
- Email notification.
- Telegram notification.

Notifications may initially be implemented as a simple background job and improved later.

---

# 6. Explicitly Out of Scope for MVP

The AI Agent MUST NOT implement these features unless explicitly requested.

- Shopping cart.
- E-commerce.
- Product inventory.
- Product checkout.
- Online payment.
- POS.
- Accounting.
- Payroll.
- Complex CRM.
- Loyalty points.
- Membership packages.
- AI chatbot.
- AI recommendations.
- Multi-branch management.
- Subscription billing.
- Advanced analytics.
- Native mobile applications.

These may be considered future features.

---

# 7. Technology Stack

## 7.1 Backend

- Java 17+
- Spring Boot 4.1.1
- Spring Security 7.1.1
- JJWT 0.12.5
- Spring Data JPA
- Hibernate
- Bean Validation
- Maven

---

## 7.2 Database

- MySQL
- Flyway

Database naming convention:

```text
snake_case
```

Examples:

```text
tenant_id
start_time
duration_minutes
is_active
created_at
```

---

## 7.3 Frontend

- React
- TypeScript
- Vite
- Tailwind CSS

Frontend should prioritize:

- Responsive design.
- Mobile usability.
- Accessibility.
- Reusable components.
- Clear state management.
- Consistent design system.

---

# 8. Architecture

## 8.1 Backend Architecture

Use a clear layered architecture:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

Responsibilities:

### Controller

- Receive HTTP requests.
- Validate request DTOs.
- Call services.
- Return HTTP responses.

Controllers MUST NOT contain business logic.

### Service

- Business logic.
- Transaction boundaries.
- Authorization checks where appropriate.
- Booking conflict validation.
- Tenant-aware operations.

### Repository

- Database access.
- Query methods.
- Persistence logic.

---

# 9. Backend Package Structure

Prefer feature-oriented organization while maintaining the 3-layer architecture.

Example:

```text
com.example.spabooking
│
├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   └── security
│
├── tenant
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── service
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── staff
├── customer
├── booking
│
├── common
│   ├── exception
│   ├── response
│   └── config
│
└── SpaBookingApplication.java
```

Do not create an unnecessarily complicated architecture.

The project should remain understandable to a junior developer.

---

# 10. Multi-Tenant Architecture

The application is a **multi-tenant SaaS**.

Each business is represented by a `tenant`.

Example:

```text
Tenant A
 ├── Users
 ├── Services
 ├── Staff
 ├── Customers
 └── Bookings

Tenant B
 ├── Users
 ├── Services
 ├── Staff
 ├── Customers
 └── Bookings
```

Tenant A MUST NEVER be able to access Tenant B's data.

---

## 10.1 Tenant-Owned Resources

The following resources must contain `tenant_id`:

- users
- services
- staff
- customers
- bookings

---

## 10.2 Tenant Isolation Rule

Every authenticated tenant-scoped query MUST be tenant-aware.

Bad:

```text
findBookingById(id)
```

Preferred:

```text
findBookingByIdAndTenantId(id, tenantId)
```

or equivalent tenant-safe architecture.

The Agent MUST NOT rely only on frontend filtering for tenant isolation.

Tenant isolation MUST be enforced on the backend.

---

# 11. Authentication and Authorization

## 11.1 Roles

Initial roles:

```text
OWNER
STAFF
```

---

## 11.2 OWNER

Owner can:

- Manage services.
- Manage staff.
- Manage customers.
- Manage bookings.
- View all bookings.
- Manage business settings.

---

## 11.3 STAFF

Staff can:

- View permitted bookings.
- View relevant customers.
- Update permitted booking statuses.

Staff MUST NOT automatically receive owner permissions.

---

# 12. JWT Authentication

Use:

```text
Spring Security
+
JWT
```

Authentication flow:

```text
Login
 ↓
Validate credentials
 ↓
Generate JWT
 ↓
Client stores token securely
 ↓
Client sends Authorization header
 ↓
Spring Security validates token
 ↓
Extract user + tenant + role
```

JWT should contain enough information to identify the authenticated principal and tenant context without exposing unnecessary sensitive information.

---

# 13. Database Schema

## 13.1 tenants

```text
tenants
---------
id
name
slug
phone
email
address
timezone
is_active
created_at
updated_at
```

---

## 13.2 users

```text
users
---------
id
tenant_id
username
password
role
staff_id
is_active
created_at
updated_at
```

Relationship:

```text
tenant 1 ──── N users
```

A staff user may be associated with a staff profile.

---

## 13.3 services

```text
services
---------
id
tenant_id
name
description
duration_minutes
price
is_active
created_at
updated_at
```

---

## 13.4 staff

```text
staff
---------
id
tenant_id
name
phone
email
is_active
created_at
updated_at
```

---

## 13.5 customers

```text
customers
---------
id
tenant_id
name
phone
email
last_visit
created_at
updated_at
```

---

## 13.6 bookings

```text
bookings
---------
id
tenant_id
customer_id
service_id
staff_id
start_time
end_time
status
is_reminded
version
created_at
updated_at
```

---

# 14. Database Relationships

```text
Tenant
 ├── Users
 ├── Services
 ├── Staff
 ├── Customers
 └── Bookings
        ├── Customer
        ├── Service
        └── Staff
```

Each relationship must respect tenant boundaries.

A booking belonging to Tenant A MUST NOT reference a customer, service, or staff member belonging to Tenant B.

The backend MUST validate these relationships.

---

# 15. Booking Rules

## 15.1 Booking Status

Initial statuses:

```text
PENDING
CONFIRMED
COMPLETED
CANCELLED
```

---

## 15.2 Time Rules

A booking has:

```text
start_time
end_time
```

`end_time` should normally be derived from:

```text
start_time + service.duration_minutes
```

The frontend MUST NOT be trusted to calculate or enforce booking duration.

The backend should calculate and validate it.

---

# 16. Double Booking Prevention

This is a critical business rule.

The system MUST prevent two active bookings for the same staff member from overlapping.

Example:

```text
Booking A
10:00 → 11:00

Booking B
10:30 → 11:30
```

This must be rejected.

---

## 16.1 Important Concurrency Rule

`@Version` / Optimistic Locking is useful for detecting concurrent updates to the **same booking**, but it is NOT by itself sufficient to prevent two separate booking records from overlapping.

Therefore, the booking creation flow MUST include:

1. Validate tenant ownership.
2. Validate service.
3. Validate staff.
4. Calculate `end_time`.
5. Check for overlapping active bookings.
6. Perform the booking operation transactionally.
7. Use an appropriate locking/concurrency strategy where necessary.
8. Handle race conditions safely.

The Agent MUST NOT claim that simply adding:

```java
@Version
private Long version;
```

solves double booking.

---

# 17. Booking Overlap Rule

For two bookings:

```text
existing.start_time < requested.end_time
AND
existing.end_time > requested.start_time
```

means the time ranges overlap.

Cancelled bookings should not block a new booking.

The exact query and locking strategy must be implemented and tested at the database/service layer.

---

# 18. REST API Convention

All APIs should use:

```text
/api/v1
```

Example:

```text
GET    /api/v1/bookings
GET    /api/v1/bookings/{id}
POST   /api/v1/bookings
PUT    /api/v1/bookings/{id}
DELETE /api/v1/bookings/{id}
```

Use RESTful resource naming.

Avoid:

```text
/getBookings
/createBooking
/deleteBooking
```

---

# 19. DTO Rules

Do not expose JPA entities directly through API responses.

Use DTOs.

Example:

```text
BookingRequest
BookingResponse
ServiceRequest
ServiceResponse
CustomerRequest
CustomerResponse
```

This reduces coupling between database structure and API contracts.

---

# 20. Validation

Use Jakarta Bean Validation where appropriate.

Examples:

```text
@NotBlank
@NotNull
@Email
@Size
@Positive
@Min
```

Business validation belongs in the service layer.

Example:

```text
Service duration must be > 0
Price must not be negative
Booking start time must be valid
Staff must belong to current tenant
Service must belong to current tenant
```

---

# 21. Error Handling

Use centralized exception handling.

Preferred:

```text
@RestControllerAdvice
```

Return consistent error responses.

Example:

```json
{
  "timestamp": "...",
  "status": 409,
  "code": "BOOKING_CONFLICT",
  "message": "The selected time is no longer available."
}
```

Use appropriate HTTP status codes.

Examples:

```text
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error
```

Do not expose stack traces or sensitive implementation details to clients.

---

# 22. Frontend Architecture

Frontend should be organized by feature rather than putting everything into one large component directory.

Example:

```text
src/
├── features/
│   ├── auth/
│   ├── bookings/
│   ├── services/
│   ├── staff/
│   ├── customers/
│   └── dashboard/
│
├── components/
├── layouts/
├── pages/
├── hooks/
├── lib/
├── services/
├── types/
└── app/
```

Reusable UI components should be separated from feature-specific components.

---

# 23. UI/UX Principles

The product is designed for small business owners who may not be technically experienced.

Therefore:

- Avoid unnecessary complexity.
- Keep primary actions obvious.
- Minimize the number of steps for common tasks.
- Use clear Vietnamese-friendly terminology.
- Make important information visually obvious.
- Prioritize mobile responsiveness.
- Use large enough touch targets.
- Provide clear loading states.
- Provide clear empty states.
- Provide useful error messages.
- Avoid excessive animations.
- Avoid visual clutter.

The dashboard should answer:

> "What is happening today?"

within a few seconds.

---

# 24. Design System

The UI should use a consistent design system.

Define:

- Color palette.
- Typography.
- Font sizes.
- Font weights.
- Spacing scale.
- Border radius.
- Shadows.
- Buttons.
- Inputs.
- Selects.
- Cards.
- Tables.
- Calendar.
- Modal/dialog.
- Toast/notification.
- Empty states.
- Loading states.
- Error states.

Do not randomly invent new styles for individual pages.

---

# 25. UI/UX AI Skills

The project may use:

- UI UX Pro Max
- Taste Skill
- Impeccable

Recommended workflow:

```text
Product Requirements
        ↓
UI UX Pro Max
        ↓
Design System
        ↓
Taste Skill
        ↓
Visual refinement
        ↓
Frontend implementation
        ↓
Impeccable
        ↓
UI audit + polish
```

These tools should support the product requirements rather than override them.

The AI Agent MUST NOT introduce unrelated visual patterns or features simply because they look impressive.

---

# 26. Accessibility

The frontend should follow basic accessibility principles:

- Semantic HTML.
- Keyboard accessibility.
- Visible focus states.
- Sufficient contrast.
- Proper labels for form fields.
- Accessible buttons.
- Accessible dialogs.
- Meaningful error messages.
- Do not rely only on color to communicate state.

---

# 27. Responsive Design

The application should support:

```text
Mobile
Tablet
Desktop
```

Priority:

1. Mobile usability for public booking.
2. Desktop usability for owner dashboard.
3. Responsive behavior for staff.

Do not simply shrink the desktop UI for mobile.

Mobile layouts may require different component arrangements.

---

# 28. Public Booking Page

The public booking page must not require authentication.

Example URL:

```text
/book/{tenantSlug}
```

Example:

```text
/book/abc-spa
```

The page should display:

- Business name.
- Services.
- Staff.
- Available dates.
- Available time slots.
- Customer booking form.

Public users must only be able to access information explicitly intended for public booking.

Do not expose private business data.

---

# 29. Notifications

Initial notification architecture:

```text
Booking
   ↓
Notification Scheduler
   ↓
Find upcoming bookings
   ↓
Check is_reminded
   ↓
Send notification
   ↓
Mark reminder as sent
```

Use:

```text
@Scheduled
```

for the initial implementation.

Email may use:

```text
JavaMailSender
```

Telegram notifications may use the Telegram Bot API.

Notification sending should not block the main booking creation request unnecessarily.

---

# 30. Timezone

Because the initial target market is Vietnam, the default tenant timezone should be:

```text
Asia/Ho_Chi_Minh
```

However, timezone should be stored at the tenant level rather than hard-coded throughout the application.

Store and process date/time consistently.

Avoid mixing:

```text
LocalDateTime
Instant
ZonedDateTime
```

without a clear reason.

---

# 31. Security Rules

The Agent MUST:

- Hash passwords.
- Never store plain-text passwords.
- Validate JWTs.
- Enforce authorization server-side.
- Enforce tenant isolation server-side.
- Validate resource ownership.
- Avoid exposing sensitive information.
- Validate public booking input.
- Protect authenticated APIs.
- Avoid logging passwords or JWTs.
- Use environment variables for secrets.

Secrets MUST NOT be committed to Git.

Examples:

```text
JWT_SECRET
DB_PASSWORD
MAIL_PASSWORD
TELEGRAM_BOT_TOKEN
```

Use environment variables or appropriate secret configuration.

---

# 32. CORS

Configure CORS explicitly for known frontend origins.

Do NOT use:

```text
allowedOrigins("*")
```

as a permanent production configuration.

Development configuration may be different from production.

---

# 33. Database Migration

Flyway MUST be used for schema changes.

Example:

```text
V1__create_tenants.sql
V2__create_users.sql
V3__create_services.sql
V4__create_staff.sql
V5__create_customers.sql
V6__create_bookings.sql
```

Do not manually modify production schema outside migration files.

---

# 34. Indexing

Important queries should have appropriate indexes.

Potential indexes include:

```text
users(tenant_id)
services(tenant_id)
staff(tenant_id)
customers(tenant_id)
bookings(tenant_id)
bookings(staff_id, start_time, end_time)
bookings(customer_id)
bookings(status)
```

Exact indexes should be validated against actual query patterns.

Do not create indexes blindly.

---

# 35. API Security and Tenant Context

Authenticated requests should derive tenant information from the authenticated user/security context.

Do NOT trust:

```text
tenant_id
```

sent by the frontend as the authority for access control.

For example, the frontend should not be able to change:

```json
{
  "tenantId": 123
}
```

and thereby access another tenant.

The backend must determine the current tenant from the authenticated identity.

---

# 36. Testing Strategy

Testing is required for important business logic.

## Backend

At minimum:

- Service unit tests.
- Repository tests for important queries.
- Controller/API integration tests.
- Authentication tests.
- Tenant isolation tests.
- Booking conflict tests.

Critical test:

```text
Tenant A creates booking.
Tenant B attempts to access booking.
→ Must fail.
```

Critical concurrency tests should also be added for booking conflicts.

---

# 37. Definition of Done

A feature is NOT considered complete merely because the code compiles.

A feature is complete when:

- Backend logic works.
- API works.
- Validation exists.
- Authorization is correct.
- Tenant isolation is preserved.
- Errors are handled.
- Database migration exists.
- Important tests exist.
- Frontend handles loading state.
- Frontend handles empty state.
- Frontend handles error state.
- UI is responsive.
- No obvious console errors remain.
- No secrets are committed.
- Existing features still work.

---

# 38. Development Roadmap

## Phase 0 — Product & Design Foundation

Before implementation:

- Finalize product scope.
- Define user roles.
- Define user flows.
- Define booking rules.
- Define database relationships.
- Define API conventions.
- Define design system.
- Define dashboard information architecture.

Do not start with random UI coding.

---

# Phase 1 — Backend Foundation

Tasks:

- Create Spring Boot project.
- Configure Maven.
- Configure MySQL.
- Configure Flyway.
- Create initial migrations.
- Create JPA entities.
- Create repositories.
- Create DTOs.
- Create basic services.
- Create centralized exception handling.
- Configure application profiles.

Goal:

```text
Spring Boot
    ↓
MySQL
    ↓
Flyway
    ↓
JPA
```

working correctly.

---

# Phase 2 — Authentication & Multi-Tenant

Tasks:

- User registration/business creation.
- Login.
- Password hashing.
- JWT.
- Spring Security.
- OWNER role.
- STAFF role.
- Tenant context.
- Tenant isolation.
- Authorization.

Goal:

```text
User
 ↓
Authentication
 ↓
Tenant
 ↓
Role
 ↓
Authorized resources
```

---

# Phase 3 — Business Management

Implement:

- Services.
- Staff.
- Customers.

CRUD operations should be tenant-safe.

---

# Phase 4 — Booking System

Implement:

- Create booking.
- List bookings.
- Booking detail.
- Update booking.
- Cancel booking.
- Status transitions.
- Availability checking.
- Overlap detection.
- Concurrency protection.
- Booking conflict handling.

This is the most important business domain of the MVP.

---

# Phase 5 — Public Booking

Implement:

- Public tenant page.
- Service selection.
- Staff selection.
- Date selection.
- Available slots.
- Customer form.
- Booking confirmation.

No authentication required for customers.

---

# Phase 6 — Frontend Dashboard

Implement:

- Login page.
- Dashboard.
- Calendar.
- Booking management.
- Service management.
- Staff management.
- Customer management.
- Business settings.

Prioritize the most common workflows first.

---

# Phase 7 — Notifications

Implement:

- Scheduled reminder job.
- Email reminder.
- Telegram reminder.
- Reminder status.
- Failure handling.

---

# Phase 8 — Quality & Production Readiness

Review:

- Security.
- Tenant isolation.
- API consistency.
- Database indexes.
- Error handling.
- Logging.
- Validation.
- Frontend responsiveness.
- Accessibility.
- Performance.
- Test coverage.
- Environment configuration.

---

# 39. Git Rules

Use meaningful commits.

Examples:

```text
feat: add tenant entity
feat: implement jwt authentication
feat: add service management api
feat: implement booking conflict validation
feat: add public booking page
fix: prevent cross-tenant booking access
test: add booking concurrency tests
refactor: simplify tenant context handling
```

Avoid commits such as:

```text
update
fix
stuff
test
aaa
final
final2
```

---

# 40. Agent Rules

The AI Agent MUST follow these rules throughout development.

## Architecture

1. Follow the Controller → Service → Repository architecture.
2. Keep business logic out of controllers.
3. Do not expose JPA entities directly as API contracts.
4. Use DTOs.
5. Keep frontend features modular.

## Database

6. Use `snake_case` for table and column names.
7. Use Flyway for migrations.
8. Do not silently change existing database structure.
9. Consider indexes based on actual query patterns.

## API

10. Use `/api/v1`.
11. Follow RESTful naming.
12. Use appropriate HTTP status codes.
13. Return consistent error responses.

## Security

14. Never store plain-text passwords.
15. Never expose secrets.
16. Never trust frontend-provided tenant IDs for authorization.
17. Enforce tenant isolation on the backend.
18. Validate resource ownership.

## Booking

19. Validate booking conflicts on the backend.
20. Do not claim `@Version` alone prevents double booking.
21. Handle concurrent booking attempts safely.
22. Cancelled bookings should not block availability.

## Scope

23. Do not implement features outside the current phase without explicit approval.
24. Do not add e-commerce functionality.
25. Do not add unnecessary dependencies.
26. Do not introduce complex architecture without a clear reason.

## Code Quality

27. Prefer readable code over clever code.
28. Avoid premature abstraction.
29. Reuse components and services where appropriate.
30. Do not duplicate business rules across frontend and backend.
31. Run tests after meaningful changes.
32. Do not declare a feature complete without verification.

---

# 41. Agent Workflow

Before implementing a feature, the Agent should:

```text
1. Read MASTER_PLAN.md
        ↓
2. Identify the current phase
        ↓
3. Understand affected business rules
        ↓
4. Inspect existing code
        ↓
5. Propose implementation plan
        ↓
6. Implement
        ↓
7. Test
        ↓
8. Review security / tenant isolation
        ↓
9. Review UI/UX if frontend is affected
        ↓
10. Summarize changes
```

The Agent should not blindly modify unrelated files.

---

# 42. UI/UX Agent Workflow

For frontend work:

```text
Requirements
    ↓
User Flow
    ↓
Information Architecture
    ↓
Design System
    ↓
Component Structure
    ↓
Implementation
    ↓
Responsive Review
    ↓
Accessibility Review
    ↓
Visual Polish
```

Use UI/UX skills as supporting tools, not as a replacement for product requirements.

---

# 43. MVP Success Criteria

The MVP should be able to demonstrate this complete scenario:

```text
Owner creates account
        ↓
Business/Tenant created
        ↓
Owner logs in
        ↓
Owner creates services
        ↓
Owner creates staff
        ↓
Owner opens dashboard
        ↓
Customer opens public booking URL
        ↓
Customer selects service
        ↓
Customer selects staff
        ↓
Customer selects available time
        ↓
Customer submits booking
        ↓
Backend validates tenant + service + staff + time
        ↓
Booking created
        ↓
Owner sees booking on calendar
        ↓
Owner can confirm/complete/cancel
        ↓
Reminder is sent
```

The system must maintain tenant isolation throughout this entire flow.

---

# 44. Future SaaS Direction

After the MVP proves the core workflow, potential future capabilities include:

```text
MVP
 ↓
Business Settings
 ↓
Working Hours
 ↓
Staff Schedules
 ↓
Customer History
 ↓
Analytics
 ↓
Multiple Branches
 ↓
Subscription Plans
 ↓
Online Payments
```

Future functionality must be introduced deliberately.

The product should remain a **focused booking SaaS**, not become an uncontrolled all-in-one business management platform.

---

# 45. Current Priority

The current project priority is:

> **Build a reliable, simple, multi-tenant booking system first.**

Do not optimize for feature count.

Prioritize:

```text
Correctness
    ↓
Security
    ↓
Tenant Isolation
    ↓
Booking Reliability
    ↓
Usability
    ↓
Maintainability
    ↓
Visual Polish
    ↓
Additional Features
```

The goal is to build a small product that works correctly and can realistically evolve into a SaaS, while simultaneously strengthening Java, Spring Boot, SQL, React, TypeScript, REST API, authentication, concurrency, and software architecture skills.