# ADR 0005: Strava sync is its own module, not a `DataProvider`

## Status

Accepted (Phase 1: OAuth connect + on-demand sync only — no webhook/continuous sync yet)

## Context

Suunto has no self-serve API for a personal project — its Cloud API requires becoming an approved
business partner. Manually exporting and uploading GPX files, even in bulk (multi-file upload), doesn't
scale to "keep syncing continuously." Suunto's own app already auto-pushes every future workout to
Strava if enabled (Profile → Partner services → Strava), and Strava's API was redesigned in 2026
specifically for this "Single Player Mode" use case: an app scoped to just one athlete's own data.

Every existing provider (`provider/suunto/`, and the not-yet-built Cressi/Apple Health/Renpho ones)
implements `provider.spi.DataProvider`: `bytes -> ParseResult`, built around parsing an uploaded file.
Strava activities arrive as JSON from two separate API calls (an activity summary list, then a
per-activity `/streams` call for the actual GPS/HR time series) — there is no file, and no single call
that returns everything a GPX file would in one shot.

## Decision

Strava sync is a separate module, `backend/src/main/java/com/mysportsapp/integrations/strava/` (sibling
to `provider/`, not a package under it) — not a `DataProvider` implementation. It produces the same
normalized `ParsedActivity`/`ParsedTrackPoint` DTOs any provider does, so it plugs into the existing
dedup/persist pipeline unchanged: `ImportService.importFile()` was split so its parse-agnostic second
half (dedupe → persist → record the `ImportBatch` outcome) is now `ImportService.persistParsedActivities()`,
called by both the file-upload path and `StravaSyncService`.

Key pieces: `StravaConnection` (one per user, OAuth tokens encrypted at rest via
`EncryptedStringConverter` — AES/GCM, keyed by `STRAVA_TOKEN_ENCRYPTION_KEY`), `StravaOAuthService`
(authorization-code flow; see its javadoc for how the OAuth `state` parameter substitutes for a session
cookie, since this app's JWT lives in localStorage, not a cookie, and can't survive Strava's own
redirect), `StravaApiClient` (token refresh + the two API calls), `StravaActivityMapper`
(Strava JSON → `ParsedActivity`, normalizing `type` to the same vocabulary GPX activities use where
there's a clear equivalent), and `StravaSyncService` (pages through history, tolerates a rate limit or a
single activity's streams failing without losing the rest of the sync).

**Phase 1 scope, deliberately**: OAuth connect + an on-demand "Sync now" button pulling full history.
No webhook subscription/endpoint yet (continuous, automatic sync), no calories (only on Strava's
"detailed" per-activity representation — a third API call per activity this phase skips), no
disconnect/revoke UI. All three are real, known gaps to revisit, not oversights — see the plan this ADR
came from for the reasoning on phasing webhooks separately.

## Consequences

- Adding Strava did not require touching `provider/`, `DataProviderRegistry`, `DedupService`, or
  `ActivityRepository` at all — exactly the point of extracting `persistParsedActivities`. Any future
  non-file import source (another vendor's API) follows the same pattern rather than being forced through
  `DataProvider`.
- This does introduce the app's first genuinely external, authenticated API dependency and its first
  encrypted-at-rest column — real new surface area (token refresh failure modes, a public unauthenticated
  `/callback` endpoint, a new secret to rotate) that the file-upload providers never had to consider.
- Deploying this to production requires configuring `STRAVA_CLIENT_ID`/`STRAVA_CLIENT_SECRET`/
  `STRAVA_TOKEN_ENCRYPTION_KEY`/`STRAVA_REDIRECT_URI`/`STRAVA_FRONTEND_URL` — deliberately **not** wired
  into `.github/workflows/ci.yml`'s deploy job yet, since referencing not-yet-created Secret Manager
  entries would break every future deploy until they exist. See `docs/deployment.md` for the setup, to be
  done once Phase 1 is verified against a real Strava account.
