# ADR 0003: Explicit provider selection at upload time, not file auto-detection

## Status
Accepted

## Context
Each upload needs to be routed to the right `DataProvider` implementation. Two options: auto-detect the
provider from file content/extension, or have the user pick it explicitly in the upload UI. Several
providers (Cressi, Renpho) export CSV, which is structurally ambiguous to distinguish from content alone,
especially with only one real-world sample file to build a detector against.

## Decision
The user explicitly selects the provider from a dropdown (populated from `GET /api/v1/imports/providers`,
never hardcoded in the frontend) when uploading. `DataProvider.canParse()` still runs server-side after
selection as a validation guard — if the file doesn't match the chosen provider, the import fails clearly
(422) rather than silently misparsing.

## Consequences
- No confidence-scored auto-detection logic to build or maintain.
- The user always knows which device produced a file, so this isn't a real usability cost.
- Adding a new provider only requires implementing `DataProvider` and registering it as a Spring bean —
  the frontend dropdown updates automatically since it reads the registry via the API.
