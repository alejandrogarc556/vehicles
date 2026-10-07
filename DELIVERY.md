# Delivery

## Included

- Spring Boot 3 backend in `testias`.
- Angular 20 frontend in `testias-front`.
- PostgreSQL Docker Compose configuration supplied by the project.
- Flyway schema, sample catalog data, REST API, persistence, concurrency handling, idempotency, and validation.
- Backend and frontend automated test sources.
- Assessment documentation: `README.md`, `ARCHITECTURE_DECISIONS.md`, `ACCEPTANCE_CRITERIA.md`, and `AI_USAGE.md`.

## Run

Backend:

```powershell
cd testias
docker compose up -d postgres
gradlew.bat bootRun
```

Frontend:

```powershell
cd testias-front
npm install
npm start
```

The backend listens on port `8082`. The Angular development proxy forwards `/api` requests to it.

## Review checklist

- Confirm PostgreSQL is running before starting the backend.
- Verify Flyway completes and sample vehicles are available.
- Verify catalog pagination and brand filtering.
- Verify a successful reservation changes the vehicle to `RESERVED`.
- Verify concurrent reservations produce one confirmation and one rejection.
- Verify same-reference replay and different-data conflict behavior.
- Verify lookup distinguishes unknown references from rejected reservations.
- Verify restart persistence while retaining the PostgreSQL volume.
- Run backend and frontend tests in the local environment.

## Assumptions

- The application runs locally as one backend instance.
- PostgreSQL is the authoritative persistent store.
- The frontend and backend are developed on the same machine.
- No authentication, authorization, payments, notifications, external integrations, or distributed scaling are required.

## Limitations

- No administrative catalog management API.
- No production deployment, authentication, authorization, or high availability.
- The development proxy is configured for local frontend-to-backend communication.
- Full test execution has not been represented as successful unless confirmed in the delivery environment.
