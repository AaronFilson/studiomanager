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

output "mobile_env" {
  description = "Put in mobile/.env.local. Add EXPO_PUBLIC_API_BASE_URL: the api_url output of envs/<env>, or the API on your machine. None of these is secret."
  value = {
    EXPO_PUBLIC_ENTRA_CLIENT_ID = azuread_application.mobile.client_id
    EXPO_PUBLIC_ENTRA_ISSUER    = "https://${var.external_tenant_subdomain}.ciamlogin.com/${var.external_tenant_id}/v2.0"
    EXPO_PUBLIC_API_SCOPES      = "${azuread_application_identifier_uri.api.identifier_uri}/access_as_user"
  }
}

output "authority" {
  description = "VITE_ENTRA_AUTHORITY for the web app."
  value       = "https://${var.external_tenant_subdomain}.ciamlogin.com/"
}
