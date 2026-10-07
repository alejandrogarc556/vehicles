# AI Context Document
## Vehicle Reservation System - Technical Assessment

## Objective

Build a local prototype for a vehicle reservation process.

The system must allow sales advisors to:

1. Browse vehicles.
2. Reserve a vehicle.
3. Query reservation results.

The main business risks are:

- Double reservation of the same vehicle.
- Duplicate requests caused by retries.
- Loss of reservation state after application restart.

The solution must prioritize business consistency over performance.

---

# Business Rules

## BR-01

A physical vehicle can only be reserved once.

## BR-02

Two concurrent requests for the same vehicle cannot both succeed.

## BR-03

The same referenceId must never generate multiple business effects.

## BR-04

If the same referenceId is received again with identical data, return the original result.

## BR-05

If the same referenceId is received with different business data, return a controlled conflict response and preserve the original result.

## BR-06

Reservation results must remain available after backend restart.

---

# Functional Requirements

## FR-01 Vehicle Catalog

The system must allow sales advisors to browse the available vehicle catalog.

The catalog must:

- Display vehicle information.
- Support pagination.
- Support filtering by brand.
- Return results using a stable ordering.
- Support navigation through available result pages.

Each vehicle must expose at least:

- Identifier
- Brand
- Model
- Year
- Price
- Availability status

The solution must be capable of handling catalog growth without requiring all vehicles to be loaded in a single request.

---

## FR-02 Create Reservation Request

The system must allow a sales advisor to request a reservation for an available vehicle.

A reservation request must contain:

- Vehicle identifier
- Buyer identifier
- Request reference

All fields are mandatory.

The request reference represents a business operation and must be preserved throughout the reservation lifecycle.

The reservation must always use the vehicle price currently stored in the catalog.

Reasonable validation rules for user input must be defined and documented.

---

## FR-03 Process Reservation Request

The system must evaluate and resolve every valid reservation request.

If:

- The vehicle exists.
- The vehicle is available.
- The request data is valid.

Then the reservation may be confirmed.

If the reservation is confirmed:

- The vehicle must no longer be available for future reservations.
- The result must be persisted.
- The reservation outcome must be returned.

If the reservation cannot be confirmed:

- A controlled business outcome must be returned.
- The reason must be preserved when applicable.

Only one successful reservation may exist for the same physical vehicle.

If multiple reservation requests compete for the same vehicle, at most one request may be confirmed.

All valid processed reservation requests must be retained, including unsuccessful ones.

Invalid requests must return a controlled error and must not modify the catalog state.

---

## FR-04 Request Replay and Idempotency

The system must safely handle repeated requests.

If a previously processed reference is received again with the same business information:

- The original result must be returned.
- No new reservation may be created.
- No additional business side effects may occur.

This behavior applies to both successful and unsuccessful reservation outcomes.

If a previously processed reference is received with different business information:

- The original result must remain unchanged.
- The system must return a controlled response.
- The system must preserve traceability of the original request.

At minimum, business equivalence must consider:

- Vehicle identifier
- Buyer identifier

Concurrent submissions using the same reference must never produce more than one business effect.

---

## FR-05 Reservation Lookup

The system must allow users to retrieve a processed reservation using its request reference.

The response must provide enough information to understand the reservation outcome.

Returned information should include:

- Vehicle information
- Buyer identifier
- Reservation result
- Reason when applicable

The system must clearly distinguish between:

- An unknown reference.
- An existing reservation that was not confirmed.

Reservation information must remain available after application restart.

---

## FR-06 Reliable User Experience

The user interface must provide a reliable representation of backend state.

The application must:

- Indicate loading states.
- Display reservation outcomes.
- Display understandable error messages.
- Allow retrying failed operations.

The user interface must never display a successful reservation unless the backend has confirmed it.

If a user changes catalog filters while previous requests are still in progress:

- The visible results must correspond to the active filter.
- Older responses must not overwrite newer filter results.

If a reservation response is lost or unavailable:

- The user must be able to retry using the same reference.
- The user must be able to retrieve the reservation outcome.

The user interface must remain consistent with the state persisted by the backend.

# Non-Functional Requirements

Priority order:

1. Consistency
2. Correctness
3. Idempotency
4. Persistence
5. Simplicity
6. Performance

System is not distributed.

No horizontal scaling required.

No high availability required.

Local execution only.

---

# Technology Stack

Backend:

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL

Frontend:

- Angular 20+
- RxJS

Architecture:

- Clean Architecture
- Hexagonal Architecture
- DDD Lite

---

# Domain Model

## Vehicle

Attributes:

- id
- brand
- model
- year
- price
- availability
- version

Availability:

- AVAILABLE
- RESERVED

---

## Reservation

Attributes:

- referenceId
- vehicleId
- buyerId
- status
- reason
- createdAt

Status:

- CONFIRMED
- REJECTED

---

# Concurrency Strategy

## Problem

Two advisors may reserve the same vehicle simultaneously.

## Evaluated Options

### Pessimistic Locking

Pros:

- Strong consistency

Cons:

- More complexity
- Database blocking

### Optimistic Locking

Pros:

- Simple
- Supported by JPA
- Good fit for low contention

Cons:

- Requires conflict handling

## Decision

Use Optimistic Locking.

Implementation:

```java
@Version
private Long version;
```

Expected behavior:

Request A -> CONFIRMED

Request B -> REJECTED

Never:

Request A -> CONFIRMED
Request B -> CONFIRMED

---

# Idempotency Strategy

referenceId is the business idempotency key.

Create unique constraint:

```sql
UNIQUE(reference_id)
```

Flow:

1. Search referenceId.
2. If exists:
    - return original result.
3. Otherwise:
    - process reservation.
    - persist result.
    - return response.

---

# Persistence Strategy

Use PostgreSQL.

Reasons:

- Reservation history survives restart.
- Supports constraints.
- Supports concurrency control.
- Realistic production behavior.

Do not use in-memory storage.

---

# API Design

## Vehicles

```http
GET /api/vehicles
```

Query params:

```http
?page=0
&size=20
&brand=Toyota
```

---

## Create Reservation

```http
POST /api/reservations
```

Request:

```json
{
  "referenceId": "REF-001",
  "vehicleId": "VM-001",
  "buyerId": "BUY-001"
}
```

---

## Query Reservation

```http
GET /api/reservations/{referenceId}
```

---

# Validation Rules

## referenceId

- Required
- Max 50 chars

## buyerId

- Required
- Max 50 chars

## vehicleId

- Required
- Must exist

---

# Acceptance Criteria

## AC-01

Available vehicle:

Result:

CONFIRMED

Vehicle becomes RESERVED.

---

## AC-02

Concurrent reservations for same vehicle:

Result:

1 CONFIRMED

1 REJECTED

---

## AC-03

Same referenceId + same data:

Result:

Original response returned.

No new reservation created.

---

## AC-04

Same referenceId + different data:

Result:

Controlled conflict.

Original record remains unchanged.

---

## AC-05

Backend restart:

Vehicle state persists.

Reservation result persists.

---

# Testing Strategy

Unit Tests:

- ReservationService
- VehicleService
- IdempotencyValidation

Integration Tests:

- Reservation creation
- Reservation lookup
- Pagination
- Filtering

Concurrency Test:

Use:

- ExecutorService
- CountDownLatch

Verify:

1 CONFIRMED

1 REJECTED

---

# Scope Restrictions

Do NOT implement:

- Authentication
- Authorization
- Payments
- Financing
- Image upload
- Notifications
- Kafka
- Redis
- Microservices
- External integrations

Stay strictly within assessment scope.

---

# AI Instructions

You are a Staff Software Engineer.

Before generating code:

1. Read this entire document.
2. Follow all business rules.
3. Prioritize consistency over performance.
4. Respect Clean Architecture boundaries.
5. Keep implementation simple.
6. Do not add out-of-scope features.
7. Generate production-quality code.

Success criteria:

- No duplicate reservations.
- Idempotent requests.
- Persistent state.
- Passing automated tests.
- Clean and maintainable architecture.