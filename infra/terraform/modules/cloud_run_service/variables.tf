variable "region" {
  description = "GCP region"
  type        = string
}

variable "service_name" {
  description = "Cloud Run service name"
  type        = string
}

variable "image" {
  description = "Container image URL"
  type        = string
}

variable "service_account_email" {
  description = "Service account email for Cloud Run"
  type        = string
}

variable "cloud_sql_connection_name" {
  description = "Cloud SQL instance connection name"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
}

variable "db_user" {
  description = "Database user name"
  type        = string
}

variable "db_password_secret_id" {
  description = "Secret Manager secret ID for DB password"
  type        = string
}

variable "jwt_secret_secret_id" {
  description = "Secret Manager secret ID for JWT secret"
  type        = string
}

variable "admin_password_hash_secret_id" {
  description = "Secret Manager secret ID for admin password hash"
  type        = string
}

variable "min_instances" {
  description = "Minimum number of instances"
  type        = number
  default     = 0
}

variable "max_instances" {
  description = "Maximum number of instances"
  type        = number
  default     = 2
}
