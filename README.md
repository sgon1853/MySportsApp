# MySportsApp

A personal, multi-tenant sports & health tracking app. Upload data exported from your devices/apps
(a Suunto watch, a dive computer, Apple Health, a smart scale), it gets deduplicated and stored, and the
app shows the visualization that makes sense for that kind of data — a map + trend charts for a GPS
activity, a depth profile for a dive, a weight trend for scale data, and so on.

Runs the same way locally and on any cloud host: a Spring Boot REST API, a React SPA, and Postgres, all in
Docker containers with no cloud-provider-specific dependencies.

## Status

**Phase 1 (current):** full architecture in place, one data provider fully working end to end —
**Suunto / GPX** file uploads. See [`docs/adr/`](docs/adr/) for the key architecture decisions and
[`CLAUDE.md`](CLAUDE.md) for how the codebase is organized and how to extend it.

**Not yet built:** Cressi Donatello, Apple Health, and Renpho providers (the plugin pattern that makes
adding them straightforward already exists — see "Adding a new data provider" in `CLAUDE.md`); email
delivery for admin invites (invite links are relayed manually for now).

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot (Web, Security, Data JPA, Flyway), Postgres |
| Frontend | React + TypeScript (Vite), TanStack Query, React Router, Recharts, Leaflet |
| Auth | JWT (stateless), admin-invited users, no public registration |
| Testing | JUnit 5 + Testcontainers + ArchUnit (backend), Vitest + React Testing Library + MSW (frontend), Playwright (e2e) |
| Deployment | Docker Compose locally; Google Cloud Run + Neon Postgres in production, deployed via CI/CD on every merge to `master` |

## Running it locally with Docker Compose (closest to production)

```bash
cp .env.example .env   # then edit .env with real values (at minimum change the passwords/JWT_SECRET)
docker compose -f deploy/docker-compose.yml --env-file .env up --build
```

- Frontend: http://localhost:8081
- Backend API: http://localhost:8080/api/v1
- Log in with the `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` you set in `.env` — that account is
  created automatically the first time the backend starts.

Stop it with `docker compose -f deploy/docker-compose.yml down` (add `-v` to also wipe the Postgres
volume).

## Running it for local development (faster inner loop)

Start only Postgres in Docker, then run backend and frontend directly with hot reload:

```bash
cp .env.example .env
docker compose -f deploy/docker-compose.yml --env-file .env up postgres -d

cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# in a second terminal
cd frontend
npm install
npm run dev
```

Frontend dev server: http://localhost:5173 (proxies `/api` to the backend on port 8080).

## Testing

```bash
# Backend: unit tests + Testcontainers integration tests (requires Docker running)
cd backend && ./mvnw verify

# Frontend: lint, unit/component tests, production build
cd frontend && npm run lint && npm run test && npm run build

# End-to-end (requires the full docker compose stack running — see above)
cd frontend && npm run e2e
```

All three run in CI on every push/PR — see [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Deploying to production

Every push to `master` that passes CI deploys automatically to Google Cloud Run (backend + frontend as
separate services, backed by a free Neon Postgres). Deploys are gated: each new revision is smoke-tested
on its own private URL before it receives any production traffic, so a bad deploy never reaches users —
see [`docs/deployment.md`](docs/deployment.md) for the one-time cloud setup this depends on and how
rollback/rotation works.

## Importing your data

1. Log in, go to **Upload**.
2. Pick the provider that matches where the file came from (only providers the backend actually supports
   appear in the dropdown).
3. Pick the exported file and submit. You'll see how many records were parsed, inserted, and skipped as
   duplicates.
4. Go to **Activities** — the new data appears automatically.

Currently supported: **Suunto Race S**, exported as a `.gpx` file (from the Suunto app, or via Strava/
Garmin Connect-style GPX export if you route your Suunto data through another platform).

## Repository layout

```
backend/    Spring Boot REST API (Java 21, Maven)
frontend/   React + TypeScript SPA (Vite)
deploy/     docker-compose.yml — the whole stack, for local use
docs/adr/   Architecture Decision Records
docs/deployment.md   One-time cloud setup (GCP + Neon) for the production deploy pipeline
.github/    CI + CD workflow
CLAUDE.md   How this codebase is organized, and how to safely extend it (new providers, new
            visualizations) — read this before making non-trivial changes, by hand or with AI assistance
docs/architecture.md   Diagrams of the system (component, deployment, data model, key flows)
```
