# Vehicle Reservation System

Backend implementation for the vehicle reservation assessment.

## Architecture

The backend uses Spring Boot, Spring Data JPA, PostgreSQL, and Flyway.

- `domain`: vehicle and reservation entities and business enums.
- `repository`: JPA persistence ports.
- `service`: catalog and reservation use cases.
- `api`: REST controllers, DTOs, validation, and error handling.
- `db/migration`: database schema and sample catalog data.

Reservation mutations are isolated in a transactional service. Vehicle updates use JPA optimistic locking through the `version` field.

## Requirements

- Java 21
- Docker with Docker Compose
- PostgreSQL supplied by the included Compose configuration

## Run locally

From the backend directory:

```powershell
cd testias
docker compose up -d postgres
gradlew.bat bootRun
```

The API starts on `http://localhost:8082`.

Flyway creates the schema and inserts sample vehicles on startup. Data is stored in the `postgres_data` Docker volume and survives backend restarts.

Initial catalog data uses COP prices:

| ID | Brand | Model | Year | Price (COP) | Initial availability |
|---|---|---|---:|---:|---|
| VM-001 | Andina | Urbano | 2024 | 80,000,000 | AVAILABLE |
| VM-002 | Andina | Urbano | 2024 | 80,000,000 | AVAILABLE |
| VM-003 | Pacífico | Ruta | 2025 | 110,000,000 | AVAILABLE |
| VM-004 | Pacífico | Familiar | 2023 | 95,000,000 | AVAILABLE |
| VM-005 | Sierra | Camino | 2025 | 130,000,000 | AVAILABLE |

VM-001 and VM-002 are separate physical vehicles even though their catalog information is identical. The catalog migration updates metadata without resetting existing availability or deleting reservations.

To stop PostgreSQL:

```powershell
docker compose down
```

The named volume is preserved. Remove it only when database data should be discarded:

```powershell
docker compose down -v
```

## API

### List vehicles

```http
GET /api/vehicles?page=0&size=20&brand=Toyota
```

`page` is zero-based. `size` must be between 1 and 100. Results are ordered by vehicle identifier.

### Create a reservation

```http
POST /api/reservations
Content-Type: application/json
```

```json
{
  "referenceId": "REF-001",
  "vehicleId": "VM-001",
  "buyerId": "BUY-001"
}
```

All fields are required and limited to 50 characters. A missing vehicle returns `404`. An unavailable vehicle is retained as a `REJECTED` reservation with a reason. A successful reservation returns `CONFIRMED` and changes the vehicle to `RESERVED`.

### Query a reservation

```http
GET /api/reservations/REF-001
```

An unknown reference returns `404`. Existing rejected reservations are returned with their status and reason.

## Business rules

- A physical vehicle can be confirmed only once.
- Every processed valid request is retained, including rejected outcomes.
- The vehicle price comes from the catalog at reservation time.
- Repeating a reference with identical vehicle and buyer data returns the original result without new side effects.
- Reusing a reference with different vehicle or buyer data returns `409 CONFLICT`; the original record is unchanged.

## Concurrency and idempotency

Vehicles contain a JPA `@Version` field. Concurrent attempts to reserve the same available vehicle cause one transaction to update the vehicle successfully. The competing transaction receives an optimistic-lock failure and stores a rejected reservation.

`reference_id` has a unique database constraint, enforcing idempotency in PostgreSQL. The service checks existing references before processing and handles concurrent unique-key races by replaying the committed result or returning a conflict.

The reservation table also has a generated technical `id` primary key. `reference_id` remains a separate unique business key, so idempotency is enforced without coupling the database identity to an externally supplied value.

## Verification scenarios

1. Reserve an available vehicle: expect `CONFIRMED`; query the catalog and expect `RESERVED`.
2. Submit two different references for the same vehicle concurrently: expect exactly one `CONFIRMED` and one `REJECTED`.
3. Repeat a confirmed or rejected request with the same reference, vehicle, and buyer: expect the original result and no new record.
4. Repeat a request with an existing reference but different vehicle or buyer: expect `409 CONFLICT`; verify the original result is unchanged.
5. Query an unknown reference: expect `404`; query a rejected reference: expect `200` with `REJECTED` and its reason.
6. Restart the backend while keeping the PostgreSQL volume: verify reservation outcomes and vehicle availability remain unchanged.

These scenarios cover FR-01 through FR-05 and BR-01 through BR-06. Frontend reliability requirements are intentionally outside this backend delivery.

## Testing

Start the database and backend before running integration or concurrency tests. Flyway creates and validates the database schema during backend startup.

From `testias`:

```powershell
docker compose up -d postgres
gradlew.bat bootRun
```

Leave the backend running, open another terminal, and run:

```powershell
cd testias
gradlew.bat test
```

Tests include service-level catalog/idempotency coverage and a PostgreSQL-backed concurrency scenario using `ExecutorService` and `CountDownLatch`. PostgreSQL must be running for the Spring integration tests.

## Assumptions

- The system is a single local Spring Boot instance; no distributed coordination is required.
- Vehicle identifiers are stable business identifiers supplied by the catalog.
- Reference and buyer identifiers are case-sensitive strings.
- The included migration provides representative sample vehicles only.

## Limitations

- No authentication or authorization.
- No vehicle administration or catalog editing API.
- No payments, financing, notifications, images, or external integrations.
- No horizontal scaling or high-availability design.
- The frontend is outside this backend delivery step.

## AI usage log

AI assistance was used to:

- Translate `specification.md` into the backend domain, persistence, service, and REST layers.
- Add Flyway schema/sample data and PostgreSQL configuration.
- Implement optimistic-locking concurrency and reference-based idempotency handling.
- Add focused unit and concurrency test scaffolding.
- Generate and review this assessment documentation.

The implementation and documentation require human review, especially database behavior and test results in the local environment.
