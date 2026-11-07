output "container_app_name" {
  value = azurerm_container_app.bridge.name
}

output "settings" {
  description = "The bridge's plain (non-secret) environment variables."
  value       = local.env
}

output "issuer" {
  description = "The bridge's issuer; Entra's custom OIDC identity provider uses it and <issuer>/.well-known/openid-configuration."
  value       = local.issuer
}
