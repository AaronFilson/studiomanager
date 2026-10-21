# The deploy workflow reads these with `terraform output -json`; keep them non-sensitive.

output "resource_group_name" {
  value = data.azurerm_resource_group.env.name
}

output "api_url" {
  value = module.api.url
}

output "api_container_app_name" {
  value = module.api.container_app_name
}

output "migration_job_name" {
  value = module.api.migration_job_name
}

output "static_web_app_name" {
  value = azurerm_static_web_app.web.name
}

output "static_web_app_hostname" {
  value = azurerm_static_web_app.web.default_host_name
}

output "api_identity_name" {
  description = "Its Postgres role name. Create it once with pgaadauth_create_principal (docs/runbooks/first-deploy.md)."
  value       = azurerm_user_assigned_identity.api.name
}

output "migrator_identity_name" {
  value = azurerm_user_assigned_identity.migrator.name
}

output "postgres_fqdn" {
  value = module.postgres.fqdn
}

output "database_name" {
  value = module.postgres.database_name
}

output "db_admin_group_name" {
  value = var.db_admin_group.display_name
}

output "web_build_env" {
  description = "Build-time settings for the web app (VITE_* variables are public by design)."
  value = {
    VITE_ENTRA_CLIENT_ID = var.entra_web_client_id
    VITE_ENTRA_AUTHORITY = "https://${var.entra_external_tenant_subdomain}.ciamlogin.com/"
    VITE_API_SCOPES      = "api://${var.entra_api_client_id}/access_as_user"
    VITE_API_BASE_URL    = module.api.url
  }
}
