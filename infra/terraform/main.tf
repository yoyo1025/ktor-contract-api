terraform {
  required_version = ">= 1.9"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 6.0"
    }
  }

  backend "gcs" {
    # bucket is configured via -backend-config or environment variable
    # e.g. terraform init -backend-config="bucket=my-tfstate-bucket"
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
  ])

  service            = each.value
  disable_on_destroy = false
}

# --------------------------------------------------
# Service Account for Cloud Run
# --------------------------------------------------
resource "google_service_account" "cloud_run" {
  account_id   = "cloud-run-app"
  display_name = "Cloud Run App Service Account"
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
  secret_id = "db-password"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret" "jwt_secret" {
  secret_id = "jwt-secret"

  replication {
    auto {}
  }

  depends_on = [google_project_service.apis["secretmanager.googleapis.com"]]
}

resource "google_secret_manager_secret" "admin_password_hash" {
  secret_id = "admin-password-hash"

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
# Modules
# --------------------------------------------------
module "artifact_registry" {
  source = "./modules/artifact_registry"

  region        = var.region
  repository_id = "ktor-contract-api"

  depends_on = [google_project_service.apis["artifactregistry.googleapis.com"]]
}

module "cloud_sql" {
  source = "./modules/cloud_sql_postgres"

  region        = var.region
  instance_name = "ktor-contract-db"
  tier          = var.db_tier
  db_name       = var.db_name
  db_user       = var.db_user
  db_password   = data.google_secret_manager_secret_version.db_password.secret_data

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
  cloud_sql_connection_name     = module.cloud_sql.instance_connection_name
  db_name                       = var.db_name
  db_user                       = var.db_user
  db_password_secret_id         = google_secret_manager_secret.db_password.secret_id
  jwt_secret_secret_id          = google_secret_manager_secret.jwt_secret.secret_id
  admin_password_hash_secret_id = google_secret_manager_secret.admin_password_hash.secret_id
  min_instances                 = var.cloud_run_min_instances
  max_instances                 = var.cloud_run_max_instances

  depends_on = [
    google_project_service.apis["run.googleapis.com"],
    google_secret_manager_secret_iam_member.cloud_run_db_password,
    google_secret_manager_secret_iam_member.cloud_run_jwt_secret,
    google_secret_manager_secret_iam_member.cloud_run_admin_password_hash,
  ]
}
