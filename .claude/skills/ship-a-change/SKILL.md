---
name: ship-a-change
description: The standard end-to-end workflow for implementing any non-trivial change in this repo — branching, verification discipline, documentation upkeep, and the PR/CI process. Use for any code, config, infra, or pipeline change, not just new features.
---

# Shipping a change in MySportsApp

This is how changes get made in this repo, distilled from how the codebase, CI/CD pipeline, and its
docs were actually built and debugged. Follow it for anything non-trivial — a one-line typo fix doesn't
need a branch, a new feature or a real bug fix does.

## 1. Branch off `master`

`master` is protected by a ruleset requiring `backend`, `frontend`, and `e2e` checks to pass and a PR to
merge — direct pushes are rejected. Always `git checkout -b <type>/<short-description>` first
(`fix/...`, `docs/...`, `feature/...`).

**Before branching for anything deploy/pipeline-related**, make sure your branch point is actually
current (`git fetch origin && git rebase origin/master` if `master` moved since you last synced) — a
stale base produces a real merge conflict that can silently block GitHub from even scheduling CI for the
PR (this happened once; the fix was `git rebase origin/master`, which auto-drops any commit whose
content already landed via a squash-merge).

## 2. Implement, then actually verify — don't infer that it works

This is the single most important habit in this repo's history: every real bug in the CI/CD pipeline
(a JPQL null-parameter type-inference error, `/actuator/health` accidentally requiring auth, a CORS
rule rejecting same-origin-proxied requests, `--no-traffic` failing on a service's first deploy, nginx
crashing because a proxy target didn't resolve) was found by **running the actual system and
reproducing the failure**, not by reading the code and reasoning it should work. Reading code tells you
what it's supposed to do; running it tells you what it does.

Concretely:
- Run the relevant test suite locally before considering anything done — see CLAUDE.md's "Commands
  quick reference" (`./mvnw verify`, `npm run lint && npm run test && npm run build`, `npm run e2e`
  against the full docker-compose stack).
- If you're fixing a bug, reproduce it first (curl the failing endpoint, run the failing e2e spec, spin
  up the actual container) so you know your fix actually closes the gap, not just that it looks right.
- For anything touching auth, CORS, networking, or container/deploy config, a unit test passing is not
  sufficient evidence — these are exactly the areas where "should work" and "does work" diverge (proxies
  rewrite headers, browsers send headers you don't expect, container runtimes reject flags you assumed
  were fine). Bring up `docker compose` and hit it for real when a change touches these.
- Add a regression test for every bug you find this way — see the pattern in
  `backend/src/test/java/com/mysportsapp/config/` (`HealthEndpointIntegrationTest`,
  `CorsConfigurationIntegrationTest`) for what "test that reproduces the specific failure mode" looks
  like, not just "test that the happy path works."

## 3. Update docs in the same change, not as a follow-up

CLAUDE.md's "Documentation maintenance" section is the checklist — `docs/architecture.md`,
`README.md`, `docs/adr/`, `docs/deployment.md`, `docs/usage.md` (use the `provider-usage-docs` skill for
that one specifically), and CLAUDE.md itself. Only touch the ones the change actually affects; don't pad
a small fix with unrelated doc churn.

## 4. Commit messages explain *why*, and what you verified

Not just what changed. A good commit message in this repo's history names the root cause, why the fix
addresses it, and what was actually run to confirm it (e.g. "reproduced the 403 locally... confirmed the
fix resolves it via direct curl and a full Playwright run against the live stack"). That record is what
let later debugging build on earlier findings instead of re-discovering them.

## 5. Push, open a PR, wait for checks

```bash
git push -u origin <branch>
gh pr create --title "..." --body "## Summary\n...\n## Test plan\n- [x] ..."
gh pr checks <number>           # or watch: see below
```

For a PR whose checks are already running, watch it without polling by hand:

```bash
gh pr checks <number> --watch --interval 15
```

## 6. Merge: use judgment about who should

- **Self-merge via `gh pr merge --squash --delete-branch`** when you're mid-iteration on something
  operational (CI/CD pipeline fixes, a broken deploy, docs typos) where round-tripping through the user
  for every small fix would slow down converging on a working state, and the change carries no real
  product-decision weight.
- **Leave it for the user to review and merge** for anything that's a product/content decision (new
  features, user-facing copy, architecture choices) even if it's fully tested and low-risk — the fact
  that CI is green doesn't mean it isn't the user's call to make.
- Either way, report the PR link and the check results, not just "done."

## 7. Clean up

```bash
git checkout master && git pull
git branch --merged master | grep -v '^\*\|master' | xargs -r git branch -d
```

## Also see

- `provider-usage-docs` skill — the specific structure for `docs/usage.md` when a provider's export
  flow, supported files, or visualization changes.
- `docs/deployment.md` — the same "runnable script over inline chat instructions" preference applies to
  any multi-step CLI/cloud setup: write it to a file the user can execute, don't paste a long command
  block into a chat prompt they can't easily copy from.
