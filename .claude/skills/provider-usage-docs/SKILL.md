---
name: provider-usage-docs
description: Write or update the end-user "export from your device, import into MySportsApp" guide (docs/usage.md) for a data provider. Use whenever a new DataProvider is implemented, or an existing provider's export flow, supported file types, or visualization changes.
---

# Provider usage documentation

MySportsApp keeps one living, user-facing guide — `docs/usage.md` — with one section per data provider,
each following the same three-part structure. This skill is that structure plus the checklist for
keeping it accurate.

## The three-part structure (see the Suunto section in `docs/usage.md` for a worked example)

1. **Export from the device/app** — concrete, numbered steps for getting a file out of the source
   device/app.
2. **Import into MySportsApp** — the Upload flow: log in → Upload → pick the provider from the dropdown
   → pick the file → submit → check Activities. This step doesn't change per-provider; only deviate from
   it if the actual UI has changed.
3. **What you'll see** — which visualization the imported data resolves to, and what the summary card
   shows.

## Rules

- **Never guess the export flow from memory.** Device/app export UIs change over time. Look it up
  (official support docs, or a web search) before writing the steps, and cite the source. If you can't
  verify current steps, say so explicitly in the doc rather than presenting a guess as fact.
- **Never guess the app's own behavior either.** Verify against the actual code before describing it:
  - Provider ID / display name: `getProviderId()` / `getDisplayName()` on the `DataProvider`
    implementation (`backend/src/main/java/com/mysportsapp/provider/<name>/`).
  - Supported file types: `getSupportedFileExtensions()`.
  - Visualization: `activity/VisualizationTypeResolver.java` (or the equivalent resolver for a non-activity
    data kind) — describe what it actually resolves to, and cross-check the frontend's `ChartRegistry`
    for what that visualization type actually renders (map, which charts, which summary fields).
  - Note real limitations you find along the way (e.g. Suunto: GPS-less workouts can only export FIT,
    not GPX, so they can't be imported).
- **Keep `README.md`'s "Importing your data" section a short pointer to `docs/usage.md`**, not a
  duplicate of the detail — one source of truth, updated in one place.
- **This is one of the docs CLAUDE.md's "Documentation maintenance" checklist already requires updating**
  for a new/changed provider — if that checklist ever stops mentioning `docs/usage.md`, fix the checklist
  too, don't just silently update the doc.

## Steps

1. Read the provider's actual code (`provider/<name>/`, `VisualizationTypeResolver`, and the frontend's
   `ChartRegistry`/detail page) to ground the doc in real behavior.
2. Research the current export flow for that specific device/app if you don't already have verified,
   current steps in hand.
3. Add a new `##` section to `docs/usage.md` (or update an existing one) following the three-part
   structure above.
4. Confirm `README.md`'s pointer to `docs/usage.md` still reads correctly (it usually needs no change —
   only touch it if the overall import flow itself changed, not just which providers exist).
