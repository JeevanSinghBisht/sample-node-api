# Country / State / City CRUD API

Build a Spring Boot 3.x application (Java 21) in this repository with MySQL.

## Stack

- Spring Boot 3.3+, Java 21, Maven
- spring-boot-starter-web, spring-boot-starter-data-jpa, validation
- MySQL 8, Flyway migrations
- README with run instructions and sample curl

## Domain

- **Country**: id, code (ISO 3166-1 alpha-2, unique), name, active, createdAt, updatedAt
- **State**: id, country (FK), code, name, active, timestamps
- **City**: id, state (FK), name, active, timestamps

## REST API — base `/api/v1`

**Country:** GET/POST `/countries`, GET/PUT/DELETE `/countries/{id}`

**State:** GET/POST `/countries/{countryId}/states`, GET/PUT/DELETE `/states/{id}`

**City:** GET/POST `/states/{stateId}/cities`, GET/PUT/DELETE `/cities/{id}`

Prefer soft-delete (`active=false`). Use `@ControllerAdvice` for error JSON (400/404/409).

## Database

- Flyway `V1` schema with FK indexes
- `application.yml` env: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_DATABASE` (default `geo_crud`)
- App port **8080** (do not conflict with platform 8081/8082)

## Tests

- At least one Spring MVC or integration smoke test for Country CRUD

## Deliverable

- MR to `main` (do not merge)
- MR body: `Closes #<issue>` and `Task-Id: <from prompt>`
- README: DB setup, env vars, sample create country → state → city

Package layout: controller, service, repository, entity.
