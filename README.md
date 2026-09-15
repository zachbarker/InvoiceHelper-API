# InvoiceFlow API

Backend for **InvoiceFlow**, a freelancer invoicing SaaS built to demonstrate full-stack
engineering: real auth, role-based permissions, and a simulated paid-tier billing model.

> 🚧 Work in progress. See the [companion frontend](https://github.com/YOUR_USERNAME/invoiceflow-web)
> and commit history for current status.

## Stack

- Java 21 · Spring Boot 3 (Web, Security, Data JPA, Validation)
- PostgreSQL + Flyway migrations
- JWT-based auth (access + refresh tokens)
- JUnit 5 + H2 for tests
- Docker + GitHub Actions CI

## Local development

Requires JDK 21, Maven, and Docker.

```bash
# start a local Postgres instance
docker compose up -d

# run the app (uses application.yml defaults, pointing at the local DB)
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Running tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database via the `test` Spring profile — no Postgres
instance required for CI or local test runs.

## Project structure

```
src/main/java/com/invoiceflow/
  ├── auth/           # registration, login, JWT issuing
  ├── organization/    # organizations, memberships, roles
  ├── client/          # client CRUD
  ├── invoice/          # invoices, line items, PDF generation
  ├── billing/          # plan limits, simulated upgrade flow
  └── common/           # shared config and exception handling
```

## Data model

See [`src/main/resources/db/migration/V1__init_schema.sql`](src/main/resources/db/migration/V1__init_schema.sql)
for the full schema: organizations, users, role-based memberships, clients, invoices,
invoice line items, and subscriptions.

## Roadmap

- [x] Project scaffold, schema, CI
- [ ] Auth (registration, login, JWT)
- [ ] Client & invoice CRUD
- [ ] Role-based access control
- [ ] Billing/plan-limit simulation
- [ ] PDF invoice generation
- [ ] Deployed demo
