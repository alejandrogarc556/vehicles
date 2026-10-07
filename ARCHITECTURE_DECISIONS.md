# Architecture Decisions

## Layered DDD-lite backend

- Problem: Keep domain rules, persistence, use cases, and HTTP concerns separated in a small local system.
- Alternatives: Monolithic controllers; full ports-and-adapters framework; layered DDD-lite.
- Decision: Use `domain`, `repository`, `service`, and `api` packages.
- Justification: This matches the assessment architecture while keeping the prototype simple.
- Benefits: Clear responsibilities and straightforward testing.
- Tradeoffs: Repository interfaces are directly backed by Spring Data rather than fully separated infrastructure adapters.
- Risks: The boundaries are conventions and can weaken as the codebase grows.
- Validation: Source structure under `testias/src/main/java/com/example/testias` and service/controller tests.

## PostgreSQL with Flyway

- Problem: Reservation and vehicle state must survive backend restarts and support database constraints.
- Alternatives: In-memory storage; Hibernate schema generation; PostgreSQL with versioned migrations.
- Decision: Use PostgreSQL and Flyway migrations in `src/main/resources/db/migration`.
- Justification: PostgreSQL provides durable state, foreign keys, unique constraints, and transactional concurrency behavior.
- Benefits: Persistent history, repeatable schema setup, and production-like consistency.
- Tradeoffs: Local startup requires PostgreSQL and migration management.
- Risks: A migration failure prevents application startup.
- Validation: `V1__create_reservation_schema.sql`, `V2__use_technical_reservation_key.sql`, and `application.properties`.

## Optimistic vehicle locking

- Problem: Two advisors must not confirm the same physical vehicle concurrently.
- Alternatives: Pessimistic database locking; application synchronization; JPA optimistic locking.
- Decision: Use `Vehicle.version` with JPA `@Version`.
- Justification: The system is local with low contention, and JPA can detect a stale vehicle update without long-held database locks.
- Benefits: At most one vehicle update wins; the losing transaction can be converted into a persisted rejection.
- Tradeoffs: Conflict handling is required and rejected persistence occurs in a separate transaction.
- Risks: Incorrect exception handling could lose the rejection or expose a database error.
- Validation: `ReservationTransactionService.process`, `persistRejectedAfterConcurrency`, and `ReservationConcurrencyIntegrationTest`.

## Technical reservation identity plus business idempotency key

- Problem: `referenceId` must be unique for idempotency but is supplied business data, not an ideal technical database identity.
- Alternatives: Use `reference_id` as the primary key; use a generated technical ID plus a unique reference.
- Decision: Use generated `Reservation.id` as the primary key and unique `reference_id` as the business key.
- Justification: Technical identity and business identity have different responsibilities while preserving the required uniqueness guarantee.
- Benefits: Stable relational identity and explicit idempotency constraint.
- Tradeoffs: Lookup repositories must use `findByReferenceId` rather than the JPA primary-key method.
- Risks: Existing databases require the V2 migration before the new entity mapping can validate.
- Validation: `Reservation`, `ReservationRepository`, and `V2__use_technical_reservation_key.sql`.

## Angular request sequencing

- Problem: An older catalog response must not replace results for a newer filter or page.
- Alternatives: Allow responses to arrive freely; cancel with RxJS streams; ignore responses using request sequencing.
- Decision: Increment a catalog request token and apply a response only when its token is current.
- Justification: It is small, explicit, and sufficient for the single-page workflow.
- Benefits: Visible catalog state always corresponds to the latest request.
- Tradeoffs: Older HTTP requests still consume network resources until completion.
- Risks: Future concurrent workflows need their own sequencing or cancellation policy.
- Validation: `App.loadVehicles` and the stale-response test in `testias-front/src/app/app.spec.ts`.
