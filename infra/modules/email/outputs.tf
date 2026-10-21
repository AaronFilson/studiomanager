output "communication_service_id" {
  value = azurerm_communication_service.this.id
}

output "endpoint" {
  value = "https://${azurerm_communication_service.this.hostname}"
}

output "sender_address" {
  value = "DoNotReply@${azurerm_email_communication_service_domain.azure_managed.from_sender_domain}"
}
