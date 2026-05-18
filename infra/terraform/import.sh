#!/bin/bash
set -e

PROJECT="contract-system-496712"
REGION="asia-northeast1"
SA="contract-api-runner@${PROJECT}.iam.gserviceaccount.com"

echo "=== APIs ==="
terraform import 'google_project_service.apis["run.googleapis.com"]' "${PROJECT}/run.googleapis.com"
terraform import 'google_project_service.apis["sqladmin.googleapis.com"]' "${PROJECT}/sqladmin.googleapis.com"
terraform import 'google_project_service.apis["artifactregistry.googleapis.com"]' "${PROJECT}/artifactregistry.googleapis.com"
terraform import 'google_project_service.apis["secretmanager.googleapis.com"]' "${PROJECT}/secretmanager.googleapis.com"

echo "=== Service Account ==="
terraform import google_service_account.cloud_run "projects/${PROJECT}/serviceAccounts/${SA}"

echo "=== Secret Manager ==="
terraform import google_secret_manager_secret.db_password "projects/${PROJECT}/secrets/contract-db-password"
terraform import google_secret_manager_secret.jwt_secret "projects/${PROJECT}/secrets/contract-jwt-secret"
terraform import google_secret_manager_secret.admin_password_hash "projects/${PROJECT}/secrets/contract-admin-password-hash"

echo "=== Secret Manager IAM ==="
terraform import google_secret_manager_secret_iam_member.cloud_run_db_password "projects/${PROJECT}/secrets/contract-db-password roles/secretmanager.secretAccessor serviceAccount:${SA}"
terraform import google_secret_manager_secret_iam_member.cloud_run_jwt_secret "projects/${PROJECT}/secrets/contract-jwt-secret roles/secretmanager.secretAccessor serviceAccount:${SA}"
terraform import google_secret_manager_secret_iam_member.cloud_run_admin_password_hash "projects/${PROJECT}/secrets/contract-admin-password-hash roles/secretmanager.secretAccessor serviceAccount:${SA}"

echo "=== Artifact Registry ==="
terraform import module.artifact_registry.google_artifact_registry_repository.this "projects/${PROJECT}/locations/${REGION}/repositories/contract-api"

echo "=== Cloud SQL ==="
terraform import module.cloud_sql.google_sql_database_instance.this "projects/${PROJECT}/instances/contract-postgres"
terraform import module.cloud_sql.google_sql_database.this "projects/${PROJECT}/instances/contract-postgres/databases/contract_system"
terraform import module.cloud_sql.google_sql_user.this "${PROJECT}/contract-postgres/appuser"

echo "=== Cloud Run ==="
terraform import module.cloud_run.google_cloud_run_v2_service.this "projects/${PROJECT}/locations/${REGION}/services/ktor-contract-api"
terraform import module.cloud_run.google_cloud_run_v2_service_iam_member.public "projects/${PROJECT}/locations/${REGION}/services/ktor-contract-api roles/run.invoker allUsers"

echo ""
echo "=== Import complete! ==="
echo "Next: terraform plan"
