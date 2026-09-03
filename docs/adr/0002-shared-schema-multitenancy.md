# ADR 0002: Shared schema with user_id scoping, not schema-per-tenant

## Status
Accepted

## Context
The app must keep each user's data isolated ("multitenant"). Options considered: a separate Postgres
schema (or database) per tenant, vs. a single shared schema with a `user_id` column on every tenant-owned
table. This is a personal-scale app (one primary user, a handful of invited people at most), not a
SaaS product being sold to many organizations.

## Decision
Shared schema. `user_id` on `users` doubles as the tenant boundary — there is no separate `tenants` table.
Every tenant-owned table (`activities`, `import_batches`, and future `dive_logs`/`body_measurements`/
`vital_samples`) has a `user_id` foreign key, and repositories only expose tenant-scoped lookups
(`findByIdAndUserId`, never bare `findById`), enforced by an ArchUnit test
(`arch/TenantScopingArchTest`).

## Consequences
- One set of migrations, one connection pool, trivial local dev setup.
- Isolation is enforced in application code (repository method shape + ArchUnit), not by the database
  itself via separate schemas — a bug in a new feature *could* leak across tenants if it bypasses the
  scoped repository methods. The ArchUnit test plus `TenantIsolationIntegrationTest` (two users, assert
  cross-user 404) are the safety net; keep both when adding new tenant-owned tables.
- If a future need for stronger isolation appears (e.g. a paid multi-org product), introducing a distinct
  `tenant_id` separate from `user_id` is possible without a full rewrite, since scoping is already
  centralized rather than scattered.
