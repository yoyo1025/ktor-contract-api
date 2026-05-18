resource "google_cloud_run_v2_service" "this" {
  name     = var.service_name
  location = var.region

  template {
    service_account = var.service_account_email

    vpc_access {
      egress = "PRIVATE_RANGES_ONLY"

      network_interfaces {
        network    = "default"
        subnetwork = "default"
      }
    }

    containers {
      image = var.image

      ports {
        container_port = 8080
      }

      env {
        name  = "APP_ENV"
        value = "prod"
      }

      env {
        name  = "DB_HOST"
        value = var.db_host
      }

      env {
        name  = "DB_PORT"
        value = "5432"
      }

      env {
        name  = "DB_NAME"
        value = var.db_name
      }

      env {
        name  = "DB_USER"
        value = var.db_user
      }

      env {
        name = "DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = var.db_password_secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = var.jwt_secret_secret_id
            version = "latest"
          }
        }
      }

      env {
        name  = "JWT_ISSUER"
        value = "ktor-contract-api"
      }

      env {
        name  = "JWT_AUDIENCE"
        value = "ktor-contract-api"
      }

      env {
        name  = "JWT_EXPIRES_IN_SECONDS"
        value = "3600"
      }

      env {
        name = "ADMIN_PASSWORD_HASH"
        value_source {
          secret_key_ref {
            secret  = var.admin_password_hash_secret_id
            version = "latest"
          }
        }
      }

      resources {
        limits = {
          cpu    = "1000m"
          memory = "512Mi"
        }
        cpu_idle          = true
        startup_cpu_boost = true
      }

      startup_probe {
        tcp_socket {
          port = 8080
        }
        failure_threshold     = 1
        initial_delay_seconds = 0
        period_seconds        = 240
        timeout_seconds       = 240
      }
    }
  }
}
