# ADR 0004: JSONB column for track/profile points, not a normalized child table

## Status
Accepted

## Context
GPS activities (and later dive profiles) carry a variable-length series of points (timestamp, lat/lon,
elevation, HR / depth). This could be a normalized child table (`activity_track_points`, one row per
point) or a single `JSONB` column on the parent row.

## Decision
Store the point series as `JSONB` on the parent row (`activities.track_points`). No child table, no
join, no N+1 fetch when rendering a chart — the whole track is fetched in the same query as the activity.

## Consequences
- Simpler queries, simpler entity mapping, good enough performance at personal-app scale (dozens to low
  thousands of activities, hundreds to low thousands of points each).
- Per-point analytics across activities (e.g. "average HR at any point where elevation > X across all
  rides") would require unpacking JSONB in SQL or doing it application-side — not currently a requirement.
- Reversible later: normalizing into a child table is an internal repository-layer change invisible to
  provider code (providers only ever produce `ParsedTrackPoint` DTOs), so this isn't a one-way door.
