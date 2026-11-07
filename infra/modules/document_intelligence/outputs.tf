output "id" {
  value = azurerm_cognitive_account.this.id
}

output "endpoint" {
  description = "The API's STUDIOMANAGER_INVOICES_AZURE_ENDPOINT."
  value       = "https://${azurerm_cognitive_account.this.custom_subdomain_name}.cognitiveservices.azure.com"
}
