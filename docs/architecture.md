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
        ImportSvc["imports<br/>ImportService<br/>(orchestrator)"]
        Registry["provider<br/>DataProviderRegistry"]
        Dedup["dedup<br/>DedupService"]
        ActivitySvc["activity<br/>ActivityService /<br/>VisualizationTypeResolver"]

        subgraph Providers["provider/&lt;name&gt; (pure, DB-free)"]
            Suunto["provider/suunto<br/>SuuntoGpxProvider"]
            Future["provider/... <br/>(Cressi, Apple Health, Renpho<br/>— not yet implemented)"]
        end
    end

    DB[("Postgres<br/>(users, import_batches,<br/>activities)")]

    API -->|HTTPS + JWT| Sec
    Upload --> API
    Activities --> API
    Auth --> API

    Sec --> AuthSvc
    Sec --> ImportCtrl
    Sec --> ActivitySvc

    ImportCtrl --> ImportSvc
    ImportSvc --> Registry
    Registry --> Suunto
    Registry -.-> Future
    ImportSvc --> Dedup
    ImportSvc --> DB
    ActivitySvc --> DB
    AuthSvc --> DB

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

## 3. Data model

Every tenant-owned table carries `user_id` and is only ever queried scoped to it — see
[`../CLAUDE.md`](../CLAUDE.md) principle 2 and `TenantScopingArchTest`. Reflects the current Flyway
migrations (`V1`–`V3`).

```mermaid
erDiagram
    USERS ||--o{ IMPORT_BATCHES : "uploads"
    USERS ||--o{ ACTIVITIES : "owns"
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
```

`(user_id, dedup_key)` is unique on `activities` — that constraint, not application logic alone, is
the final backstop against double-importing the same record (see
[`adr/0004-jsonb-track-storage.md`](adr/0004-jsonb-track-storage.md) for why raw track data is stored
as JSONB rather than a normalized table).

## 4. Provider plugin pattern

Adding a provider means implementing one interface and annotating it `@Component` — see "Recipe: adding
a new data provider" in [`../CLAUDE.md`](../CLAUDE.md). No other class is touched.

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
