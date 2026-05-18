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
  default     = "db-f1-micro"
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

variable "cloud_run_image" {
  description = "Container image for Cloud Run (e.g. REGION-docker.pkg.dev/PROJECT/REPO/IMAGE:TAG)"
  type        = string
}

variable "cloud_run_min_instances" {
  description = "Minimum number of Cloud Run instances"
  type        = number
  default     = 0
}

variable "cloud_run_max_instances" {
  description = "Maximum number of Cloud Run instances"
  type        = number
  default     = 2
}
