# ADR 0001: Single Maven module for the backend

## Status
Accepted

## Context
Data providers (Suunto, and later Cressi/Apple Health/Renpho) need clear boundaries so adding one doesn't
risk breaking another. A multi-module Maven reactor (one module per provider) would enforce that boundary
at compile time, but adds reactor build ordering, more POMs to maintain, and slower IDE navigation — real
costs for a solo/personal project.

## Decision
Use a single Maven module (`backend/`) with strict package boundaries (`provider.<name>` per provider) and
an ArchUnit fitness-function test enforcing tenant-scoping rules. Provider isolation is a *package*
convention backed by a test, not a build-system-enforced module boundary.

## Consequences
- Simpler build, faster local iteration, one `pom.xml`.
- Provider isolation relies on the ArchUnit test staying meaningful — if it's ever weakened or removed,
  the boundary becomes purely social convention. Revisit if providers are ever published/versioned
  independently, which isn't a current goal.
