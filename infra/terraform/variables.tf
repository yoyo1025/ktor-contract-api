variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region"
  type        = string
  default     = "asia-northeast1"
}

variable "db_tier" {
  description = "Cloud SQL instance tier"
  type        = string
  default     = "db-perf-optimized-N-8"
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "contract_system"
}

variable "db_user" {
  description = "Database user name"
  type        = string
  default     = "appuser"
}

variable "db_host" {
  description = "Cloud SQL private IP address"
  type        = string
}

variable "cloud_run_image" {
  description = "Container image for Cloud Run (e.g. REGION-docker.pkg.dev/PROJECT/REPO/IMAGE:TAG)"
  type        = string
}
