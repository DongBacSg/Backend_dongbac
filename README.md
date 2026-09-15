# Dong Bac Sai Gon Backend

Backend Phase 1 foundation for ĐÔNG BẮC SÀI GÒN.

This project is a Spring Boot modular monolith prepared for two future frontends:

- Public frontend: https://dong-bac-group.vercel.app/
- Admin frontend: https://admin-dongbac-lhzv.vercel.app/

The frontends are not connected in this phase.

## Stack

- Java 17
- Spring Boot 3.5.16
- Maven Wrapper
- Spring Web
- Spring Data JPA
- PostgreSQL JDBC
- Flyway
- Bean Validation
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI

No Spring Security, JWT, Cloudinary SDK, Supabase SDK, Redis, queues, GraphQL, or business APIs are included in Phase 1.

## Architecture

The backend is prepared as a modular monolith under:

```text
com.dongbacsaigon.backend
├── common
│   ├── config
│   ├── exception
│   ├── response
│   ├── util
│   └── web
├── auth
├── user
├── approval
├── media
├── site
├── catalog
├── article
├── contact
└── audit
```

Feature packages are module boundaries only. Business logic and business tables are intentionally deferred.

Future API route conventions:

- Public APIs: `/api/public/**`
- Admin APIs: `/api/admin/**`

Phase 1 includes only `GET /api/public/ping` as an infrastructure endpoint.

## Database Strategy

The application connects directly to Supabase PostgreSQL through JDBC.

Use the Supabase Session Pooler:

```text
jdbc:postgresql://<SESSION_POOLER_HOST>:5432/postgres?sslmode=require
```

Application schema:

```text
dongbac
```

Flyway owns schema changes from this phase onward. Hibernate is configured with:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate must not create or update business schema automatically.

Startup validates `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_SCHEMA` before the DataSource is created. `DB_URL` must be a PostgreSQL JDBC URL and include `sslmode=require`.

Initial migration:

```text
src/main/resources/db/migration/V1__initialize_dongbac_schema.sql
```

It only creates the `dongbac` schema if needed. No business tables are created.

## Environment Variables

Required:

```text
SPRING_PROFILES_ACTIVE
DB_URL
DB_USERNAME
DB_PASSWORD
DB_SCHEMA
CORS_ALLOWED_ORIGINS
```

Optional:

```text
PORT
DB_POOL_MAX_SIZE
DB_POOL_MIN_IDLE
DB_CONNECTION_TIMEOUT_MS
DB_POOL_VALIDATION_TIMEOUT_MS
DB_POOL_IDLE_TIMEOUT_MS
DB_POOL_MAX_LIFETIME_MS
```

`.env.example` documents safe placeholder values only. Spring Boot does not automatically load arbitrary `.env` files; set variables through PowerShell, your IDE, Render, or another explicit mechanism.

## Local Windows Setup

Verify Java:

```powershell
java -version
```

Verify Maven Wrapper:

```powershell
.\mvnw.cmd -version
```

Set environment variables in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:PORT="8080"
$env:DB_URL="jdbc:postgresql://<HOST>:5432/postgres?sslmode=require"
$env:DB_USERNAME="postgres.<PROJECT_REF>"
$env:DB_PASSWORD="<YOUR_PASSWORD>"
$env:DB_SCHEMA="dongbac"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:5174,https://dong-bac-group.vercel.app,https://admin-dongbac-lhzv.vercel.app"
```

Build:

```powershell
.\mvnw.cmd clean package
```

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

The app listens on `8080` by default and also supports Render's `PORT` environment variable.

## Verification URLs

Health:

```text
http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Infrastructure ping:

```text
http://localhost:8080/api/public/ping
```

## CORS

CORS is centralized in `common.config`.

Default allowed origins:

```text
http://localhost:5173
http://localhost:5174
https://dong-bac-group.vercel.app
https://admin-dongbac-lhzv.vercel.app
```

Override with:

```text
CORS_ALLOWED_ORIGINS
```

Wildcard origins are not used with credentials.

## Actuator

Only these endpoints are exposed:

```text
/actuator/health
/actuator/info
```

Render health-check path:

```text
/actuator/health
```

## Docker

Build:

```powershell
docker build -t dongbac-backend .
```

The Dockerfile uses a Java 17 multi-stage build and Maven Wrapper. Secrets are not baked into the image.

## Render Readiness

`render.yaml` is included as a minimal Docker web service blueprint.

Render environment variables to configure later:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=<Supabase Session Pooler JDBC URL with sslmode=require>
DB_USERNAME=postgres.<PROJECT_REF>
DB_PASSWORD=<secret>
DB_SCHEMA=dongbac
CORS_ALLOWED_ORIGINS=https://dong-bac-group.vercel.app,https://admin-dongbac-lhzv.vercel.app
```

Do not commit actual credentials.

## Current Phase Limitations

Backend Phase 1 intentionally does not include:

- Authentication
- Spring Security
- JWT
- Users, roles, or staff APIs
- Products or categories
- Articles or announcements
- Contacts or leads
- Media or Cloudinary integration
- Supabase SDK usage
- Frontend API integration

Backend Phase 2 should start only after this foundation is verified.
