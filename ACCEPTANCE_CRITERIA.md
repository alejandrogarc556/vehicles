# Acceptance Criteria

## Functional requirements coverage

| Requirement | Implemented evidence | Status |
|---|---|---|
| FR-01 Vehicle catalog | `VehicleController`, `VehicleService`, `VehicleRepository`; Angular catalog, brand filter, page navigation, stable ID ordering | Implemented |
| FR-02 Create request | `ReservationController`, `ReservationRequest`, Bean Validation, `ReservationTransactionService`; reservation form | Implemented |
| FR-03 Process request | Transactional reservation processing, vehicle availability transition, rejected reasons, persistence | Implemented |
| FR-04 Replay/idempotency | Unique `reference_id`, `findByReferenceId`, replay/conflict logic, retryable frontend form | Implemented |
| FR-05 Lookup | `GET /api/reservations/{referenceId}`, `ReservationResponse`, lookup UI | Implemented |
| FR-06 Reliable UX | Angular loading/error/retry states, backend-authoritative result display, request sequencing | Implemented |

## Business rules and acceptance scenarios

| Rule/scenario | Expected behavior | Evidence |
|---|---|---|
| BR-01 / AC-01 | A vehicle can be confirmed once and becomes `RESERVED` | `Vehicle.@Version`, `Vehicle.reserve`, reservation integration tests |
| BR-02 / AC-02 | Concurrent requests produce one `CONFIRMED` and one `REJECTED` | `ReservationConcurrencyIntegrationTest` |
| BR-03 / AC-03 | A reference creates no duplicate business effect | Unique constraint and `ReservationService.create` |
| BR-04 | Same reference and same vehicle/buyer replays original result | `replayOrConflict` and Angular retry behavior |
| BR-05 / AC-04 | Different business data returns `409`; original remains unchanged | `replayOrConflict`, `ReservationServiceTest` |
| BR-06 / AC-05 | Database-backed vehicle and reservation state survives restart | PostgreSQL volume, Flyway schema, persistent entities |

## Validation rules

- `referenceId`: required, maximum 50 characters.
- `buyerId`: required, maximum 50 characters.
- `vehicleId`: required and must identify an existing vehicle.
- Invalid request bodies return controlled validation errors and do not mutate vehicle state.

## Frontend behavior

- Catalog, filtering, reservation, lookup, loading, errors, retry, and stale-response protection are covered by `testias-front/src/app/app.spec.ts`.
- A confirmation is displayed only from a successful backend reservation response.

## Verification note

The implementation has been reviewed against the specification. Test execution depends on the local PostgreSQL, Gradle, Node, and browser tooling being available; execution results are not asserted by this document.
