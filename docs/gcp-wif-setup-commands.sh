#!/usr/bin/env bash
# One-time Workload Identity Federation setup for the GitHub Actions deploy
# pipeline. Run this once, in your own terminal, after gcloud auth login and
# gcloud config set project mysportsapp-prod. See docs/deployment.md for
# full context. Safe to delete after running.
set -euo pipefail

PROJECT_ID=mysportsapp-prod
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
GITHUB_REPO="sgon1853/MySportsApp"

gcloud iam service-accounts create github-deployer --display-name="GitHub Actions deployer"

for ROLE in roles/run.admin roles/artifactregistry.writer roles/iam.serviceAccountUser roles/secretmanager.secretAccessor; do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:github-deployer@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="$ROLE"
done

gcloud iam workload-identity-pools create github-pool \
  --location=global --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github-provider \
  --location=global --workload-identity-pool=github-pool \
  --display-name="GitHub OIDC" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${GITHUB_REPO}'" \
  --issuer-uri="https://token.actions.githubusercontent.com"

gcloud iam service-accounts add-iam-policy-binding \
  "github-deployer@${PROJECT_ID}.iam.gserviceaccount.com" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-pool/attribute.repository/${GITHUB_REPO}"

gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/secretmanager.secretAccessor"

echo
echo "Done. Workload Identity Provider resource name (save this for the GitHub variable GCP_WORKLOAD_IDENTITY_PROVIDER):"
gcloud iam workload-identity-pools providers describe github-provider \
  --location=global --workload-identity-pool=github-pool --format='value(name)'
