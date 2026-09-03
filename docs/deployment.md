# Deploying MySportsApp to Google Cloud Run + Neon

This is a one-time setup you run yourself (it creates resources under your own
Google Cloud and Neon accounts — nothing here can be done on your behalf).
Once it's done, every push to `master` that passes CI deploys automatically —
see the `deploy-backend`/`deploy-frontend` jobs in `.github/workflows/ci.yml`.

## Why this design

- **No long-lived cloud credentials in GitHub.** Auth uses Workload Identity
  Federation (WIF): GitHub's own per-run OIDC token is exchanged for a short-lived
  GCP token, scoped to this one repo. There is no service-account JSON key
  sitting in a GitHub secret waiting to leak.
- **Real secrets never touch GitHub at all.** The JWT signing secret, the
  admin bootstrap password, and the database password live only in Google
  Secret Manager. GitHub Actions only ever references their *names*.
- **A bad deploy never reaches users.** Each deploy creates a new Cloud Run
  revision with `--no-traffic`, smoke-tests it on its own private URL
  (which also proves the database connection and Flyway migrations are
  healthy, since Spring Boot's health check pings the datasource), and only
  then shifts production traffic to it. If the smoke test fails, the job
  fails and the old revision keeps serving everyone — nothing to roll back.

## 1. Google Cloud project

```bash
brew install --cask google-cloud-sdk   # if you don't have gcloud yet
gcloud auth login
gcloud projects create mysportsapp-prod --name="MySportsApp"
gcloud config set project mysportsapp-prod
```

Enable billing on the project in the [Cloud Console](https://console.cloud.google.com/billing)
— GCP requires a billing account attached even for Always Free usage, but
you won't be charged as long as you stay within the free tier. **Set a
budget alert** (Billing → Budgets & alerts) for a small amount (e.g. $1) as
a tripwire in case something outside the free tier gets used by mistake.

```bash
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  iamcredentials.googleapis.com

gcloud artifacts repositories create mysportsapp \
  --repository-format=docker \
  --location=us-central1
```

## 2. Neon (free Postgres)

1. Sign up at [neon.tech](https://neon.tech) (no card required) → create a project → create a database.
2. From the connection details, note: host, database name, username, password. Neon requires `sslmode=require`.
3. Your JDBC URL (a GitHub Actions **variable**, not a secret — it has no password in it):
   ```
   jdbc:postgresql://<neon-host>/<database>?sslmode=require
   ```

## 3. Secrets (Google Secret Manager)

Only the genuinely sensitive values go here — generate strong random values for the first two:

```bash
openssl rand -base64 48 | tr -d '\n' | gcloud secrets create jwt-secret --data-file=-
echo -n "<a strong admin password you choose>" | gcloud secrets create admin-bootstrap-password --data-file=-
echo -n "<your Neon database password>" | gcloud secrets create db-password --data-file=-
```

## 4. Workload Identity Federation + deploy service account

```bash
PROJECT_ID=mysportsapp-prod
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='value(projectNumber)')
GITHUB_REPO="sgon1853/MySportsApp"   # owner/repo, exactly as on GitHub

# Service account the deploy workflow will impersonate
gcloud iam service-accounts create github-deployer \
  --display-name="GitHub Actions deployer"

for ROLE in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser roles/secretmanager.secretAccessor; do
  gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:github-deployer@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="$ROLE"
done

# Workload Identity Pool + OIDC provider trusting GitHub Actions
gcloud iam workload-identity-pools create github-pool \
  --location=global --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github-provider \
  --location=global --workload-identity-pool=github-pool \
  --display-name="GitHub OIDC" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${GITHUB_REPO}'" \
  --issuer-uri="https://token.actions.githubusercontent.com"

# Let only THIS repo's workflow impersonate the deploy service account
gcloud iam service-accounts add-iam-policy-binding \
  github-deployer@${PROJECT_ID}.iam.gserviceaccount.com \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${GITHUB_REPO}"
```

Also grant the Cloud Run *runtime* identity (the default compute service
account, unless you configure a different one) access to read the secrets:

```bash
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"
```

Get the provider's full resource name for the next step:

```bash
gcloud iam workload-identity-pools providers describe github-provider \
  --location=global --workload-identity-pool=github-pool --format='value(name)'
```

## 5. GitHub repo configuration

**Settings → Secrets and variables → Actions → Variables tab** (none of these are secret values — auth to GCP happens via OIDC, not these):

| Variable | Value |
|---|---|
| `GCP_PROJECT_ID` | `mysportsapp-prod` |
| `GCP_REGION` | `us-central1` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | the full resource name from the last command above |
| `GCP_DEPLOYER_SA_EMAIL` | `github-deployer@mysportsapp-prod.iam.gserviceaccount.com` |
| `NEON_DATASOURCE_URL` | `jdbc:postgresql://<neon-host>/<database>?sslmode=require` |
| `NEON_DATASOURCE_USERNAME` | your Neon database username |
| `ADMIN_BOOTSTRAP_EMAIL` | the email you want as the first admin account |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | see below — this one has a chicken-and-egg wrinkle |

**About `APP_CORS_ALLOWED_ORIGIN_PATTERNS`**: the backend needs to know the frontend's origin to
accept its cross-origin calls, but the frontend's Cloud Run URL isn't assigned until it's deployed
at least once — and that first deploy is exactly what you're setting these variables up to trigger.
This only blocks *browser* calls, not the deploy pipeline itself (the automated smoke test hits
`/actuator/health` directly with `curl`, which never sends an `Origin` header), so it's safe to
bootstrap in two passes:

1. Leave this variable unset (or set to `http://localhost:*`) for the first deploy. Both services will
   deploy and pass their smoke tests normally.
2. Open the **Actions** tab, find the completed `deploy-frontend` job, and copy the URL it printed at
   the end (something like `https://mysportsapp-frontend-xxxxxxxxxx-uc.a.run.app`).
3. Set `APP_CORS_ALLOWED_ORIGIN_PATTERNS` to that exact URL, then re-trigger the workflow (an empty
   commit, or re-run it from the Actions tab) so the backend redeploys with it. From then on it stays
   correct across future deploys, since a Cloud Run service's URL doesn't change once created.

Then also set up [branch protection](../README.md) if you haven't — the
deploy jobs only run on pushes to `master`, and branch protection is what
guarantees every such push already passed the `backend`/`frontend`/`e2e`
checks.

## 6. First deploy

Merge any PR into `master` (or push directly, if branch protection isn't on
yet) and watch the **Actions** tab. `deploy-backend` and `deploy-frontend`
will build images, push them to Artifact Registry, deploy them as
untraffic'd candidate revisions, smoke-test each, and promote. The final
step of `deploy-frontend` prints the live URL.

## Operating notes

- **Logs**: `gcloud run services logs read mysportsapp-backend --region us-central1`
- **Cold starts**: both services scale to zero when idle (that's what keeps this free) — the first request after a while will be a few seconds slower. Not something to fix for a personal-use app.
- **Rolling back a *promoted* bad revision** (rare — the smoke test should catch most issues first): `gcloud run services update-traffic mysportsapp-backend --region us-central1 --to-revisions=<previous-revision-name>=100`. List revisions with `gcloud run revisions list --service mysportsapp-backend --region us-central1`.
- **Rotating a secret** (e.g. `jwt-secret`): `echo -n "<new value>" | gcloud secrets versions add jwt-secret --data-file=-`, then re-run the deploy workflow (or `gcloud run services update mysportsapp-backend --region us-central1` with no other changes) to pick up `:latest`. Rotating `jwt-secret` invalidates every issued token — all users are logged out.
