#!/usr/bin/env bash
# One-time GitHub Actions repo variables for the Cloud Run deploy pipeline.
# Run `gh auth login` first if you haven't. Safe to re-run (each `gh
# variable set` overwrites the existing value). See docs/deployment.md.
#
# Fill in the placeholders below before running, then:
#   bash docs/gh-actions-variables-setup.sh
set -euo pipefail

REPO="sgon1853/MySportsApp"

gh variable set GCP_PROJECT_ID --repo "$REPO" --body "mysportsapp-prod"
gh variable set GCP_REGION --repo "$REPO" --body "us-central1"
gh variable set GCP_DEPLOYER_SA_EMAIL --repo "$REPO" --body "github-deployer@mysportsapp-prod.iam.gserviceaccount.com"
gh variable set NEON_DATASOURCE_URL --repo "$REPO" --body "jdbc:postgresql://ep-twilight-butterfly-aypmn4k6-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require"
gh variable set NEON_DATASOURCE_USERNAME --repo "$REPO" --body "neondb_owner"

# Fill these two in before running:
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --repo "$REPO" --body "REPLACE_WITH_PROVIDER_RESOURCE_NAME"
gh variable set ADMIN_BOOTSTRAP_EMAIL --repo "$REPO" --body "REPLACE_WITH_YOUR_ADMIN_EMAIL"

echo "Done. Current variables:"
gh variable list --repo "$REPO"
