output "communication_service_id" {
  value = azurerm_communication_service.this.id
}

output "endpoint" {
  value = "https://${azurerm_communication_service.this.hostname}"
}

output "sender_address" {
  value = (
    var.send_from_custom_domain
    ? "DoNotReply@${azurerm_email_communication_service_domain.custom[0].from_sender_domain}"
    : "DoNotReply@${azurerm_email_communication_service_domain.azure_managed.from_sender_domain}"
  )
}

output "custom_domain_verification_records" {
  description = "The DNS records to add for custom_domain (domain, SPF, DKIM, DKIM2, DMARC), or null."
  value       = try(azurerm_email_communication_service_domain.custom[0].verification_records, null)
}
