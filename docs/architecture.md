# Architecture diagrams

High-level, always-current diagrams of the system. These are generated from — and must stay in sync
with — the actual code; see the "Keeping this file current" note at the bottom for the rule that
enforces that. For *why* key decisions were made, see [`docs/adr/`](adr/); for the module-by-module
guide to extending the codebase, see [`../CLAUDE.md`](../CLAUDE.md).

## 1. Component overview

How the pieces fit together at runtime. Providers are isolated plugins; nothing outside
`ImportService` is allowed to depend on a specific provider.

```mermaid
flowchart TB
    subgraph Frontend["frontend (React SPA)"]
        Upload["features/imports<br/>(UploadPage)"]
        Activities["features/activities<br/>(list + detail + charts)"]
        Auth["auth<br/>(AuthContext, route guards)"]
        API["api/*<br/>typed fetch wrappers"]
    end

    subgraph Backend["backend (Spring Boot)"]
        Sec["security<br/>JWT filter, principal"]
        AuthSvc["auth<br/>login / invite"]
        ImportCtrl["imports<br/>ImportController"]
        ImportSvc["imports<br/>ImportService<br/>(orchestrator + shared<br/>persistParsedActivities)"]
        Registry["provider<br/>DataProviderRegistry"]
        Dedup["dedup<br/>DedupService"]
        ActivitySvc["activity<br/>ActivityService /<br/>VisualizationTypeResolver"]

        subgraph Providers["provider/&lt;name&gt; (pure, DB-free)"]
            Suunto["provider/suunto<br/>SuuntoGpxProvider"]
            Future["provider/... <br/>(Cressi, Apple Health, Renpho<br/>— not yet implemented)"]
        end

        subgraph StravaMod["integrations/strava (NOT a DataProvider - see ADR 0005)"]
            StravaCtrl["StravaController<br/>connect / callback / status / sync"]
            StravaOAuth["StravaOAuthService"]
            StravaSync["StravaSyncService"]
            StravaApi["StravaApiClient<br/>(external: api.strava.com)"]
            StravaMapper["StravaActivityMapper<br/>-> ParsedActivity"]
        end
    end

    DB[("Postgres<br/>(users, import_batches,<br/>activities, strava_connections)")]
    StravaExt(["Strava API<br/>(external)"])

    API -->|HTTPS + JWT| Sec
    Upload --> API
    Activities --> API
    Auth --> API

    Sec --> AuthSvc
    Sec --> ImportCtrl
    Sec --> ActivitySvc
    Sec --> StravaCtrl

    ImportCtrl --> ImportSvc
    ImportSvc --> Registry
    Registry --> Suunto
    Registry -.-> Future
    ImportSvc --> Dedup
    ImportSvc --> DB
    ActivitySvc --> DB
    AuthSvc --> DB

    StravaCtrl --> StravaOAuth
    StravaCtrl --> StravaSync
    StravaOAuth --> DB
    StravaSync --> StravaApi
    StravaApi --> StravaExt
    StravaApi --> DB
    StravaSync --> StravaMapper
    StravaMapper --> ImportSvc

    style Future stroke-dasharray: 5 5
```

## 2. Import flow (upload → stored activity)

The core end-to-end sequence, spanning `ImportController` → `ImportService` (see
`backend/src/main/java/com/mysportsapp/imports/ImportService.java`) → the selected provider → dedup →
persistence.

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Frontend (UploadPage)
    participant IC as ImportController
    participant IS as ImportService
    participant PR as DataProviderRegistry
    participant DP as DataProvider (e.g. SuuntoGpxProvider)
    participant DD as DedupService
    participant DB as Postgres

    U->>FE: pick provider + file, submit
    FE->>IC: POST /api/v1/imports (multipart)
    IC->>IS: importFile(providerId, filename, bytes)
    IS->>DB: insert ImportBatch (status=PENDING)
    IS->>PR: findById(providerId)
    PR-->>IS: DataProvider bean
    IS->>DP: parse(inputStream, ImportContext)
    alt file unparsable
        DP-->>IS: throws ProviderParseException
        IS->>DB: ImportBatch.complete(FAILED)
        IS-->>IC: Outcome(hardParseFailure=true)
        IC-->>FE: 422 Unprocessable Entity
    else parsed OK
        DP-->>IS: ParseResult (List&lt;ParsedActivity&gt;)
        IS->>DD: split(userId, parsedActivities)
        DD->>DB: look up existing dedup_keys for user
        DD-->>IS: DedupResult (new vs. duplicate)
        IS->>DB: saveAll(new Activity rows)
        IS->>DB: ImportBatch.complete(SUCCESS/PARTIAL)
        IS-->>IC: Outcome(result)
        IC-->>FE: 200 + counts (parsed/inserted/deduped)
    end
    FE-->>U: show result, then list refreshes via Activities API
```

## 2b. Strava sync flow (Phase 1 — on-demand, not continuous)

No file, no `DataProvider` — see [ADR 0005](adr/0005-strava-integration-not-a-dataprovider.md). Connect
is a one-time OAuth handshake; Sync can be triggered on demand as often as the user likes, reusing the
same dedup/persist path a GPX upload uses via `ImportService.persistParsedActivities`.

```mermaid
sequenceDiagram
    actor U as User
    participant FE as Frontend (StravaIntegrationPage)
    participant SC as StravaController
    participant SO as StravaOAuthService
    participant SS as StravaSyncService
    participant SA as StravaApiClient
    participant ST as Strava API
    participant IS as ImportService
    participant DB as Postgres

    U->>FE: click "Connect to Strava"
    FE->>SC: POST /connect (JWT auth)
    SC->>SO: buildAuthorizeUrl(userId)
    SO-->>SC: authorize URL (state -> userId, in-memory)
    SC-->>FE: { authorizeUrl }
    FE->>ST: browser navigates to authorizeUrl
    ST-->>SC: GET /callback?code&state (no JWT - browser redirect)
    SC->>SO: completeConnection(code, state)
    SO->>ST: POST /oauth/token (exchange code)
    SO->>DB: save StravaConnection (tokens encrypted)
    SC-->>FE: 302 redirect to /integrations/strava?connected=true

    U->>FE: click "Sync now"
    FE->>SC: POST /sync (JWT auth)
    SC->>SS: sync(userId)
    SS->>DB: create ImportBatch (providerId="strava")
    loop each page of /athlete/activities
        SS->>SA: listActivities(connection, page)
        SA->>ST: GET /athlete/activities (refreshes token if near expiry)
        loop each activity on the page
            SS->>SA: getStreams(connection, activityId)
            SA->>ST: GET /activities/{id}/streams
            SS->>SS: map to ParsedActivity
        end
    end
    SS->>IS: persistParsedActivities(batch, userId, "strava", activities)
    IS->>DB: dedupe + saveAll + complete batch
    IS-->>SC: ImportBatchResultDto
    SC-->>FE: 200 + counts
```

## 3. Data model

Every tenant-owned table carries `user_id` and is only ever queried scoped to it — see
[`../CLAUDE.md`](../CLAUDE.md) principle 2 and `TenantScopingArchTest`. Reflects the current Flyway
migrations (`V1`–`V4`).

```mermaid
erDiagram
    USERS ||--o{ IMPORT_BATCHES : "uploads"
    USERS ||--o{ ACTIVITIES : "owns"
    USERS ||--o| STRAVA_CONNECTIONS : "connects (at most one)"
    IMPORT_BATCHES ||--o{ ACTIVITIES : "produced"

    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar role
        boolean active
        uuid invited_by
        varchar invite_token UK
        timestamptz invite_token_expires_at
        timestamptz created_at
    }

    IMPORT_BATCHES {
        uuid id PK
        uuid user_id FK
        varchar provider_id
        varchar original_filename
        varchar status
        int records_parsed
        int records_inserted
        int records_deduped
        int records_failed
        text error_details
        timestamptz created_at
    }

    ACTIVITIES {
        uuid id PK
        uuid user_id FK
        varchar source_provider_id
        uuid source_import_batch_id FK
        varchar activity_type
        timestamptz start_time
        bigint duration_seconds
        double distance_meters
        int avg_hr
        int max_hr
        int calories
        double elevation_gain_meters
        jsonb track_points
        varchar dedup_key
        timestamptz created_at
    }

    STRAVA_CONNECTIONS {
        uuid id PK
        uuid user_id FK "UNIQUE - one connection per user"
        bigint strava_athlete_id UK
        varchar access_token "encrypted at rest (AES/GCM)"
        varchar refresh_token "encrypted at rest (AES/GCM)"
        timestamptz token_expires_at
        timestamptz connected_at
    }
```

`(user_id, dedup_key)` is unique on `activities` — that constraint, not application logic alone, is
the final backstop against double-importing the same record (see
[`adr/0004-jsonb-track-storage.md`](adr/0004-jsonb-track-storage.md) for why raw track data is stored
as JSONB rather than a normalized table).

## 4. Provider plugin pattern

Adding a provider means implementing one interface and annotating it `@Component` — see "Recipe: adding
a new data provider" in [`../CLAUDE.md`](../CLAUDE.md). No other class is touched.

This pattern is specifically for *file-based* sources. Strava sync (§2b) deliberately isn't a
`DataProvider` — there's no file, just two API calls — see
[ADR 0005](adr/0005-strava-integration-not-a-dataprovider.md). It produces the same `ParsedActivity`
DTOs a provider would and feeds them into the same shared `ImportService.persistParsedActivities`, so
the diagram below is still the right mental model for "how does a new activity type get in" even though
Strava itself sits outside it.

```mermaid
classDiagram
    class DataProvider {
        <<interface>>
        +getProviderId() String
        +getSupportedFileExtensions() List~String~
        +canParse(filename, content) boolean
        +parse(InputStream, ImportContext) ParseResult
    }
    class DataProviderRegistry {
        -List~DataProvider~ providers
        +findById(id) Optional~DataProvider~
        +listAll() List~ProviderInfoDto~
    }
    class SuuntoGpxProvider {
        +getProviderId() "suunto-gpx"
    }
    class FutureProvider {
        <<not yet implemented>>
        Cressi / Apple Health / Renpho
    }
    class ParseResult {
        +activities List~ParsedActivity~
    }

    DataProviderRegistry o-- "*" DataProvider : autowires all beans
    DataProvider <|.. SuuntoGpxProvider
    DataProvider <|.. FutureProvider
    DataProvider ..> ParseResult : returns
```

## 5. Deployment topology

Local development uses Docker Compose end to end; production is Google Cloud Run + Neon Postgres,
deployed automatically by GitHub Actions on every merge to `master`. Full one-time setup:
[`deployment.md`](deployment.md).

```mermaid
flowchart LR
    subgraph Local["Local (docker compose)"]
        LFE["nginx + React build<br/>:8081"]
        LBE["Spring Boot<br/>:8080"]
        LDB[("Postgres")]
        LFE --> LBE --> LDB
    end

    subgraph CI["GitHub Actions (.github/workflows/ci.yml)"]
        Test["backend / frontend / e2e jobs"]
        DeployBE["deploy-backend"]
        DeployFE["deploy-frontend"]
        Test -->|pass, push to master| DeployBE
        Test -->|pass, push to master| DeployFE
    end

    WIF["Workload Identity Federation<br/>(no long-lived GCP keys)"]
    SM[("Google Secret Manager<br/>jwt-secret, admin-bootstrap-password,<br/>db-password")]

    subgraph GCP["Google Cloud Run (production)"]
        CRFE["frontend service<br/>(scales to zero)"]
        CRBE["backend service<br/>(scales to zero)"]
    end

    Neon[("Neon Postgres<br/>(free tier)")]

    DeployBE -->|auth via| WIF
    DeployFE -->|auth via| WIF
    DeployBE --> CRBE
    DeployFE --> CRFE
    CRBE -->|reads secrets| SM
    CRBE --> Neon
    CRFE -->|VITE_API_BASE_URL| CRBE

    Note["Each deploy creates a --no-traffic revision,<br/>smoke-tests /actuator/health on its private URL,<br/>then shifts traffic. A failed smoke test is never promoted."]
```

## Keeping this file current

These diagrams are documentation, not generated artifacts — there is no build step that regenerates
them, so they only stay accurate if they're updated by hand alongside the code. The rule for that
lives in [`../CLAUDE.md`](../CLAUDE.md) under "Documentation maintenance": any change to a module
boundary, the data model, the deploy topology, or a core flow (import, auth) must update the relevant
diagram here in the same change.
