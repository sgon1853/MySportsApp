# CLAUDE.md

Guidance for Claude Code (or any future contributor, human or AI) working in this repository. Read this
before making non-trivial changes. For *why* key decisions were made, see `docs/adr/`. For *how to run*
things, see `README.md`. For a diagram-level view of the system, see `docs/architecture.md`. This file
is about how to safely *extend* the codebase.

## What this app is

Multi-tenant sports/health tracking app. Users upload export files from their devices (a watch, a dive
computer, a health app, a smart scale) through a web UI; the backend parses, deduplicates, and stores the
data; the frontend visualizes it in a way appropriate to the data type. See `README.md` for current status
(which providers actually work today).

## Module map

```
backend/src/main/java/com/mysportsapp/
  config/           Spring config, security wiring, admin bootstrap on startup
  security/         JWT issuing/validation, auth filter, authenticated-principal type
  user/             User entity/repository (User IS the tenant boundary — no separate tenants table)
  auth/             Login, accept-invite, admin-invite endpoints and service
  provider/spi/     The plugin interface every data provider implements (DataProvider, ParseResult, etc.)
  provider/         DataProviderRegistry — discovers all DataProvider beans
  provider/<name>/  One package per provider (e.g. provider/suunto/) — fully self-contained
  activity/         Activity entity/repository/controller, visualization-type resolution
  dedup/            Dedup key generation + duplicate detection (provider-agnostic)
  imports/          ImportBatch (audit trail) + ImportService orchestrating parse -> dedup -> persist
  common/exception/ Global exception handling / error response shape

frontend/src/
  api/              Typed wrappers for every backend endpoint + shared TS types (the API contract)
  auth/             Auth context, route guards
  pages/            Login, accept-invite, admin-invite
  features/imports/       Upload UI
  features/activities/    List + detail pages
  features/activities/charts/  ChartRegistry + one component per visualization type
  router.tsx        All routes in one place
```

## Architecture principles — do not violate these

1. **A data provider is a pure function from bytes to DTOs.** Anything implementing
   `provider.spi.DataProvider` must not touch the database, Spring context beans (beyond being a
   `@Component` itself), or any other provider's code. This is what makes providers independently
   testable and independently addable. If you find yourself wanting a provider to look something up in
   the DB, that logic belongs in `ImportService`, not the provider.
2. **Tenant scoping is not optional and not ad hoc.** Every tenant-owned entity (an `Activity`, an
   `ImportBatch`, and any new table you add) must only ever be looked up with the caller's `userId` as
   part of the query — `findByIdAndUserId`, never a bare `findById` from service/controller code. The
   `userId` itself comes only from the authenticated principal (`SecurityContextHolder`), never from a
   path variable, query param, or request body. This is enforced by
   `backend/src/test/java/com/mysportsapp/arch/TenantScopingArchTest.java` — if you add a new
   tenant-owned table, extend that test to cover its repository too.
3. **Visualization type is decided once, on the backend, and trusted by the frontend.** The API returns a
   `visualizationType` field; the frontend's `ChartRegistry` looks it up. Don't duplicate "which chart for
   which activity type" logic in the frontend — if a new mapping is needed, it goes in
   `activity/VisualizationTypeResolver.java`.
4. **Every schema change is a Flyway migration**, never a manual `ALTER TABLE` or relying on
   `hibernate.ddl-auto`. Migrations are append-only — never edit a migration that's already been applied
   anywhere (including on your own machine's dev DB); add a new one.
5. **No secrets in code or committed config.** All credentials/keys come from environment variables
   (`.env`, gitignored — see `.env.example` for the full list). Never commit a real `.env`.

## Recipe: adding a new data provider (e.g. Cressi, Apple Health, Renpho)

1. Create a new package `backend/src/main/java/com/mysportsapp/provider/<name>/`.
2. Implement `DataProvider` (see `provider/suunto/SuuntoGpxProvider.java` for a working example):
   `getProviderId()` (a stable string key, e.g. `"cressi-csv"`), `getSupportedFileExtensions()`,
   `canParse(filename, content)`, `parse(InputStream, ImportContext) -> ParseResult`.
3. Annotate it `@Component`. That's the entire registration step — `DataProviderRegistry` autowires all
   `DataProvider` beans and the `GET /api/v1/imports/providers` endpoint (and therefore the frontend
   dropdown) picks it up automatically. No other file needs to change for the provider to become
   selectable and usable.
4. If the provider's data doesn't fit the existing `ParsedActivity` shape (e.g. a dive log or a body
   measurement), add the new DTO to `provider/spi/` (e.g. `ParsedDiveLog`) and a corresponding field to
   `ParseResult`, plus a matching entity/repository/table (new Flyway migration) and controller endpoint,
   mirroring how `activity/` is structured for `ParsedActivity`. Don't force new data shapes into the
   existing `activities` table.
5. Add real fixture files under `src/test/resources/fixtures/<provider>/` (use actual sample exports once
   available) and a parser unit test with no Spring context, modeled on
   `provider/suunto/SuuntoGpxProviderTest.java`.
6. If the new data type needs a new visualization, see the next recipe.

## Recipe: adding a new visualization type

1. Backend: add the new type to `activity/VisualizationTypeResolver.java`'s lookup (or the equivalent
   resolver for a new data kind), and make sure the relevant DTO includes the field the chart needs.
2. Frontend: create a component under `features/activities/charts/` (or a sibling `features/<kind>/charts/`
   for a non-activity data kind), and add one entry to `ChartRegistry.tsx` mapping the `visualizationType`
   string to that component. Existing entries are untouched.
3. Add a component test asserting the registry resolves your new type to your new component, and that an
   unknown type still falls back gracefully (see the existing `ChartRegistry` test).

## Guardrails for AI-assisted changes

- Never bypass a tenant-scoped repository method to "just get it working" — fix the repository method
  instead.
- Never add a table or column without a Flyway migration.
- Never commit `.env` or any real credential/secret.
- When touching a provider's parser, always add or update a fixture-based test in the same change —
  parsers are exactly the kind of code that silently breaks on real-world data variance.
- Run the full test suite (`README.md` → Testing) before considering a change done. `./mvnw verify` and
  `npm run test` are fast enough to run on every non-trivial change.
- Keep providers ignorant of each other and of the database — that boundary is what keeps "add a
  provider" cheap.

## Documentation maintenance — do this on every non-trivial change, no need to be asked

Treat these as part of the change, not a follow-up task, and do them in the same commit:

- **`docs/architecture.md`**: if the change adds/removes a module, provider, table/column, deploy
  target, or changes a core flow (import, auth), update the corresponding Mermaid diagram in that file
  (component, sequence, ER, provider class, or deployment). Adding a new provider means adding it to
  the component diagram and provider class diagram; a new Flyway migration means updating the ER
  diagram; a CI/CD or deploy topology change means updating the deployment diagram.
- **`README.md`**: update the Status section when a provider or major feature moves from "not yet
  built" to working, and the Stack table if the toolchain changes.
- **`docs/adr/`**: add a new numbered ADR (don't edit an existing one) when the change makes or reverses
  an architectural decision — a new cross-cutting pattern, a reversal of an existing ADR, a new
  infra dependency. Routine bug fixes and additive features following an existing pattern don't need one.
- **`docs/deployment.md`**: update when the one-time cloud setup steps, required GitHub
  variables/secrets, or the deploy job behavior change.
- This file (`CLAUDE.md`): update the module map, a recipe, or a principle when the change alters the
  shape they describe (a new top-level package, a new recipe step that recipe-followers would need).

If none of the above actually changed shape, skip it — don't pad a small fix with unrelated doc churn.

## Commands quick reference

See `README.md` for the full run/test instructions. Short version:

```bash
cd backend && ./mvnw verify              # backend tests (unit + Testcontainers integration)
cd frontend && npm run test              # frontend unit/component tests
cd frontend && npm run e2e               # e2e, requires the docker compose stack running
docker compose -f deploy/docker-compose.yml --env-file .env up --build   # full stack
```
