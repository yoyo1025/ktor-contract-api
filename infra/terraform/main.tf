terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }

  backend "gcs" {
    bucket = "contract-system-496712-tfstate"
    prefix = "terraform/state"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# --------------------------------------------------
# Enable required APIs
# --------------------------------------------------
resource "google_project_service" "apis" {
  for_each = toset([
    "run.googleapis.com",
    "sqladmin.googleapis.com",
    "artifactregistry.googleapis.com",
    "secretmanager.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
  ])

  service            = each.value
  disable_on_destroy = false
}

# --------------------------------------------------
# Service Account for Cloud Run
# --------------------------------------------------
resource "google_service_account" "cloud_run" {
  account_id   = "contract-api-runner"
  display_name = "contract-api-runner"
}

resource "google_project_iam_member" "cloud_run_sql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_project_iam_member" "cloud_run_log_writer" {
  project = var.project_id
  role    = "roles/logging.logWriter"
  member  = "serviceAccount:${google_service_account.cloud_run.email}"
}

# --------------------------------------------------
# Secret Manager
# --------------------------------------------------
resource "google_secret_manager_secret" "db_password" {
  secret_id = "contract-db-password"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "contract-jwt-secret"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret" "admin_password_hash" {
  secret_id = "contract-admin-password-hash"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

# Grant Cloud Run SA access to secrets
resource "google_secret_manager_secret_iam_member" "cloud_run_db_password" {
  secret_id = google_secret_manager_secret.db_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_secret_manager_secret_iam_member" "cloud_run_jwt_secret" {
  secret_id = google_secret_manager_secret.jwt_secret.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_secret_manager_secret_iam_member" "cloud_run_admin_password_hash" {
  secret_id = google_secret_manager_secret.admin_password_hash.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

# --------------------------------------------------
# GitHub Actions Deploy Service Account
# --------------------------------------------------
resource "google_service_account" "github_actions" {
  account_id   = "github-actions-deploy"
  display_name = "GitHub Actions Deploy"
}

resource "google_project_iam_member" "github_actions_run_admin" {
  project = var.project_id
  role    = "roles/run.admin"
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}

resource "google_project_iam_member" "github_actions_ar_writer" {
  project = var.project_id
  role    = "roles/artifactregistry.writer"
  member  = "serviceAccount:${google_service_account.github_actions.email}"
}

resource "google_service_account_iam_member" "github_actions_sa_user" {
  service_account_id = google_service_account.cloud_run.name
  role               = "roles/iam.serviceAccountUser"
  member             = "serviceAccount:${google_service_account.github_actions.email}"
}

# --------------------------------------------------
# Workload Identity Federation (GitHub Actions OIDC)
# --------------------------------------------------
resource "google_iam_workload_identity_pool" "github" {
  workload_identity_pool_id = "github-pool"
  display_name              = "GitHub Actions Pool"

  depends_on = [google_project_service.apis["iam.googleapis.com"]]
}

resource "google_iam_workload_identity_pool_provider" "github" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.github.workload_identity_pool_id
  workload_identity_pool_provider_id = "github-provider"
  display_name                       = "GitHub Actions Provider"

  attribute_mapping = {
    "google.subject"       = "assertion.sub"
    "attribute.actor"      = "assertion.actor"
    "attribute.repository" = "assertion.repository"
  }

  attribute_condition = "assertion.repository_owner == '${var.github_repository_owner}'"

  oidc {
    issuer_uri = "https://token.actions.githubusercontent.com"
  }
}

resource "google_service_account_iam_member" "github_actions_wif" {
  service_account_id = google_service_account.github_actions.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principalSet://iam.googleapis.com/${google_iam_workload_identity_pool.github.name}/attribute.repository/${var.github_repository}"
}

# --------------------------------------------------
# Modules
# --------------------------------------------------
module "artifact_registry" {
  source = "./modules/artifact_registry"

  region        = var.region
  repository_id = "contract-api"

  depends_on = [google_project_service.apis["artifactregistry.googleapis.com"]]
}

module "cloud_sql" {
  source = "./modules/cloud_sql_postgres"

  region          = var.region
  instance_name   = "contract-postgres"
  tier            = var.db_tier
  db_name         = var.db_name
  db_user         = var.db_user
  db_password     = data.google_secret_manager_secret_version.db_password.secret_data
  private_network = "projects/${var.project_id}/global/networks/default"

  depends_on = [google_project_service.apis["sqladmin.googleapis.com"]]
}

data "google_secret_manager_secret_version" "db_password" {
  secret = google_secret_manager_secret.db_password.id

  depends_on = [google_secret_manager_secret.db_password]
}

module "cloud_run" {
  source = "./modules/cloud_run_service"

  region                        = var.region
  service_name                  = "ktor-contract-api"
  image                         = var.cloud_run_image
  service_account_email         = google_service_account.cloud_run.email
  db_host                       = var.db_host
  db_name                       = var.db_name
  db_user                       = var.db_user
  db_password_secret_id         = google_secret_manager_secret.db_password.secret_id
  jwt_secret_secret_id          = google_secret_manager_secret.jwt_secret.secret_id
  admin_password_hash_secret_id = google_secret_manager_secret.admin_password_hash.secret_id

  depends_on = [
    google_project_service.apis["run.googleapis.com"],
    google_secret_manager_secret_iam_member.cloud_run_db_password,
    google_secret_manager_secret_iam_member.cloud_run_jwt_secret,
    google_secret_manager_secret_iam_member.cloud_run_admin_password_hash,
  ]
}
