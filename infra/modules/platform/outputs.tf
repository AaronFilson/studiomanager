output "container_app_environment_id" {
  value = azurerm_container_app_environment.this.id
}

output "container_app_environment_default_domain" {
  description = "An app with external ingress is served at https://<app name>.<this domain>."
  value       = azurerm_container_app_environment.this.default_domain
}

output "log_analytics_workspace_id" {
  value = azurerm_log_analytics_workspace.this.id
}

output "application_insights_connection_string" {
  value     = azurerm_application_insights.this.connection_string
  sensitive = true
}
