output "env_tfvars" {
  description = "Paste into envs/<env>/<env>.tfvars. None of these values is secret: they all end up in the public SPA bundle or token metadata."
  value = {
    entra_external_tenant_id        = var.external_tenant_id
    entra_external_tenant_subdomain = var.external_tenant_subdomain
    entra_api_client_id             = azuread_application.api.client_id
    entra_web_client_id             = azuread_application.web.client_id
  }
}

output "issuer_uri" {
  description = "STUDIOMANAGER_AUTH_ISSUER_URI for the API."
  value       = "https://${var.external_tenant_id}.ciamlogin.com/${var.external_tenant_id}/v2.0"
}

output "api_scope" {
  description = "VITE_API_SCOPES for the web app."
  value       = "${azuread_application_identifier_uri.api.identifier_uri}/access_as_user"
}

output "authority" {
  description = "VITE_ENTRA_AUTHORITY for the web app."
  value       = "https://${var.external_tenant_subdomain}.ciamlogin.com/"
}
