resource "google_sql_database_instance" "this" {
  name             = var.instance_name
  database_version = "POSTGRES_18"
  region           = var.region

  settings {
    tier              = var.tier
    edition           = "ENTERPRISE_PLUS"
    availability_type = "ZONAL"
    disk_size         = 100
    disk_type         = "PD_SSD"
    disk_autoresize   = false

    ip_configuration {
      ipv4_enabled    = true
      private_network = var.private_network
    }

    backup_configuration {
      enabled = false
    }

    data_cache_config {
      data_cache_enabled = true
    }

    database_flags {
      name  = "cloudsql.iam_authentication"
      value = "on"
    }
  }

  deletion_protection = true
}

resource "google_sql_database" "this" {
  name     = var.db_name
  instance = google_sql_database_instance.this.name
}

resource "google_sql_user" "this" {
  name     = var.db_user
  instance = google_sql_database_instance.this.name
  password = var.db_password
}
