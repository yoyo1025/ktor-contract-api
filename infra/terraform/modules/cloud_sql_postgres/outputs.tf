output "instance_name" {
  description = "Cloud SQL instance name"
  value       = google_sql_database_instance.this.name
}

output "instance_connection_name" {
  description = "Cloud SQL instance connection name (PROJECT:REGION:INSTANCE)"
  value       = google_sql_database_instance.this.connection_name
}

output "database_name" {
  description = "Database name"
  value       = google_sql_database.this.name
}
