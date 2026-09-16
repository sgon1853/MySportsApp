# Using MySportsApp

Where to find the app, how to log in, what you can do once you're in, and — per supported device/app —
how to get your data out of it and into MySportsApp.

See [`README.md`](../README.md) for which providers are actually implemented right now, and
[`CLAUDE.md`](../CLAUDE.md) for how to add a new one.

---

## Accessing the app

- **Production:** [mysportsapp-frontend-sbfss5jaea-uc.a.run.app](https://mysportsapp-frontend-sbfss5jaea-uc.a.run.app/)
- **Local (Docker Compose):** [localhost:8081](http://localhost:8081) — see the README's "Running it
  locally" section to start the stack.

There's no public sign-up. Every account is created by an admin — either the one bootstrapped
automatically on first startup (`ADMIN_BOOTSTRAP_EMAIL`/`ADMIN_BOOTSTRAP_PASSWORD`), or invited by an
existing admin (see "Inviting a new user" below). If you don't have credentials yet, ask whoever runs
the app for an invite.

## Logging in

Go to the app's URL, you'll land on **Login**. Enter the email and password from your account or invite.
There's no "forgot password" flow yet — ask an admin to send you a fresh invite if you're locked out.

## What you can do

### Activities

**Activities** (the landing page once you're logged in) lists every activity you've imported, across all
providers: type, date, duration, distance, and average heart rate, with a filter by activity type (e.g.
`RUN`). Click a row to open the **activity detail** page: a full stat card (distance, duration, pace,
average/max heart rate, elevation gain, calories, and which provider it came from) plus whichever
visualization fits that activity's data — today that's a GPS-track view (map + heart-rate and elevation
charts) for anything with location points; more visualization types land as new data kinds ship (see
`VisualizationTypeResolver` in `CLAUDE.md`'s module map).

### Uploading data

**Upload** is where you bring new files in — pick the provider matching where the files came from, pick
one file or several at once (e.g. your whole exported history), and submit. Multiple files upload one
after another in a single batch, and you get back both an aggregated result and a per-file breakdown of
how many records were parsed, inserted, and skipped as duplicates. Re-uploading a file you already
imported is always safe: MySportsApp deduplicates on import rather than creating repeats. See the
provider sections below for the exact export-then-import steps for each supported device/app.

### Inviting a new user (admin only)

If your account has the admin role, **Admin → Invite** lets you invite someone new: enter their email,
submit, and you get back a shareable invite link (it expires — the page shows when) rather than the
invite being emailed automatically. Send that link to the person yourself; opening it takes them to
**Accept invite**, where they set their own password and are logged in.

---

## Importing your data, by provider

Every provider's section here follows the same shape: **Export** (from the device/app) → **Import**
(into MySportsApp) → **What you'll see**. This file gets a new section every time a provider ships.

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

## Strava (connect + sync, not a file upload)

Unlike every provider above, Strava doesn't need a file at all — connect your account once, then click
**Sync now** whenever you want. This depends on Suunto's own auto-sync to Strava
(Suunto app → Profile → Partner services → Strava) — enable that first if you haven't, since MySportsApp
reads from Strava, not from Suunto directly (see [ADR 0005](adr/0005-strava-integration-not-a-dataprovider.md)
for why: Suunto has no self-serve API for a personal project).

**Phase 1 status**: connect + on-demand sync only. There's no automatic/continuous sync yet (that needs a
Strava webhook subscription, a deliberately separate follow-up), and calories aren't imported (Strava
only exposes those on a per-activity call this phase skips to stay within its rate limits comfortably).

### 0. One-time setup (you do this once, not per sync)

MySportsApp needs its own registered Strava API application before anyone can connect an account to it:

1. Log in at [strava.com](https://www.strava.com) → **Settings → My API Application** → create an app.
   As of mid-2026 this requires an active Strava subscription and creates an app limited to
   "Single Player Mode" (your own account only) — exactly the fit for a personal project like this.
2. Note the **Client ID** and **Client Secret** it gives you.
3. Set these on the backend (see `.env.example`): `STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`,
   `STRAVA_TOKEN_ENCRYPTION_KEY` (generate with `openssl rand -base64 32` — don't reuse
   `application.yml`'s checked-in default outside local dev), `STRAVA_REDIRECT_URI` (must exactly match
   what you register in the Strava app's settings), `STRAVA_FRONTEND_URL`.
4. Restart the backend. Deploying this to production additionally needs these added to Secret
   Manager/GitHub variables and wired into the deploy job — not done automatically by this phase, see
   [ADR 0005](adr/0005-strava-integration-not-a-dataprovider.md)'s Consequences section for why.

### 1. Connect your account

1. Log in to MySportsApp, go to **Strava** in the nav.
2. Click **Connect to Strava**, approve access on Strava's own consent screen.
3. You're redirected back showing "Connected".

### 2. Sync

Click **Sync now** any time. It pages through your full Strava history, so the first sync may take a
little longer than later ones — later syncs are fast since already-imported activities are recognized as
duplicates the same way a re-uploaded GPX file would be.

### 3. What you'll see

Same **GPS track** visualization as Suunto activities (map, heart-rate-over-time, elevation-over-distance)
— Strava's `type` is normalized to the same vocabulary (a Strava "Run" and a Suunto "RUNNING" activity
look the same) where there's a clear equivalent, so importing from both sources doesn't create two
different-looking categories for what's really the same kind of activity.

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
