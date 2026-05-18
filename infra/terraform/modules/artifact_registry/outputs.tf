output "repository_id" {
  description = "The repository ID"
  value       = google_artifact_registry_repository.this.repository_id
}

output "repository_url" {
  description = "The repository URL for docker push"
  value       = "${var.region}-docker.pkg.dev/${google_artifact_registry_repository.this.project}/${google_artifact_registry_repository.this.repository_id}"
}
