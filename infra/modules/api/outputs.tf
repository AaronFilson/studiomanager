output "container_app_name" {
  value = azurerm_container_app.api.name
}

output "fqdn" {
  value = azurerm_container_app.api.ingress[0].fqdn
}

output "url" {
  value = "https://${azurerm_container_app.api.ingress[0].fqdn}"
}

output "migration_job_name" {
  value = azurerm_container_app_job.migrate.name
}
