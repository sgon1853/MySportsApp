# Using MySportsApp

How to get your data out of each supported device/app and into MySportsApp, and what you'll see once
it's imported. Every provider's section here follows the same shape: **Export** (from the device/app) →
**Import** (into MySportsApp) → **What you'll see**.

See [`README.md`](../README.md) for which providers are actually implemented right now, and
[`CLAUDE.md`](../CLAUDE.md) for how to add a new one — this file gets a new section every time a provider
ships.

---

## Suunto Race S

### 1. Export a workout from the Suunto app

1. Make sure the workout has synced from your watch to the **Suunto app** on your phone (it syncs
   automatically over Bluetooth after a workout, or pull-to-refresh on the app's home/Diary screen if it
   hasn't).
2. Open the workout in the Suunto app.
3. Tap the **⋮** (three dots) in the top-right corner.
4. Choose **Export**, then pick **GPX** as the format (not FIT — MySportsApp's Suunto provider reads
   GPX only).
5. Save or share the resulting `.gpx` file somewhere you can reach it from wherever you'll log into
   MySportsApp — e.g. save to Files, AirDrop to your Mac, or email it to yourself.

**Limitation** (from Suunto's own docs): a workout recorded **without GPS** (e.g. an indoor gym session)
can only export as FIT, not GPX — those can't be imported into MySportsApp today.

Source: [Suunto — "What type of files can I export from the Suunto app?"](https://www.suunto.com/Support/faq-articles/suunto-app/what-type-of-files-can-i-export-from-the-suunto-app/)

### 2. Import it into MySportsApp

1. Log in to MySportsApp.
2. Go to **Upload**.
3. Provider: select **Suunto (GPX)**.
4. File: choose the `.gpx` file(s) you exported — you can select multiple files at once (e.g. your
   whole exported history) and they'll upload one after another in a single submission, with an
   aggregated result plus a per-file breakdown.
5. Submit — you'll immediately see how many records were parsed and inserted, in total and per file.
   Re-uploading the same workout is safe: it's recognized as a duplicate and reported as such rather
   than creating a second entry, whether it's the only file in the batch or one of many.
6. Go to **Activities** — the workout(s) appear in the list.

### 3. What you'll see

Suunto activities render as a **GPS track** visualization:
- A summary card: activity type, date, duration, distance, pace, average/max heart rate, elevation gain.
- A map of the GPS track (skipped if the file has no location points).
- A heart-rate-over-time chart and an elevation-over-distance chart, where that data is present in the
  file.

### 4. Getting your full history in at once

Suunto's *only officially documented* export is the one-workout-at-a-time flow above — there's no
supported "export everything" button in the app. For bulk-importing your whole history, in order of
what's worth trying first:

1. **Check Suunto app → Profile → Account/Settings for an "Export data" option.** Several users report
   this exists and emails you a zip of all your workouts (FIT/GPX/JSON), but it isn't documented in
   Suunto's own support articles, so treat it as "might be there" rather than guaranteed — if you find
   it, extract the `.gpx` files from the zip and skip straight to importing them (multiple at once,
   below).
2. **Request a personal data export from Suunto directly.** Email **privacy@suunto.com** asking for your
   workout data under GDPR / the EU Data Act — Suunto's own privacy notice commits to providing it in a
   structured, machine-readable format (JSON, NDJSON, GPX, FIT, or CSV). This is the one *guaranteed*
   route to everything at once, but forum reports describe turnaround taking **weeks**, not minutes — not
   useful if you want your history in today, but the reliable option if you're willing to wait.
3. **Third-party sync tools** (Runalyze, RunGap) have been reported to pull a Suunto account's full
   history in bulk. Weigh this against the trust cost of granting a third-party service access to your
   Suunto account before using one.
4. **Fall back to manual per-workout export** (step 1 above) for however many workouts are worth the
   effort, then use MySportsApp's multi-file upload to import them all in one submission instead of one
   at a time — select every exported `.gpx` file at once on the Upload page.

Sources: [Suunto Community Forum — "Bulk export FROM Suunto App?"](https://forum.suunto.com/topic/5265/bulk-export-from-suunto-app),
[Suunto Community Forum — "Download All Activity Data"](https://forum.suunto.com/topic/8703/download-all-activity-data),
[Gneta — "How to Export Your Suunto Data — and What It Leaves Out"](https://www.gneta.app/blog/export-suunto-data-guide)
(current as of research done for this doc — re-verify if it's been a while, since none of this is Suunto's
documented, stable API surface).

---

## Adding a new provider's section here

Do this as part of implementing the provider (see CLAUDE.md's "adding a new data provider" recipe), not
as an afterthought:

1. Verify the real behavior in code first — provider ID/display name (`getProviderId()`/
   `getDisplayName()`), supported file types (`getSupportedFileExtensions()`), and which visualization it
   resolves to (`VisualizationTypeResolver`) — don't describe aspirational behavior.
2. Research the *current* export flow for that specific device/app (official docs or a search) rather
   than relying on memory — export UIs change over time. Cite the source.
3. Add a new `##` section here with the same three-part structure as above.

A project skill (`.claude/skills/provider-usage-docs/`) captures this same checklist for future use.
