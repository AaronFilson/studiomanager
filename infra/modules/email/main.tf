# Email through Azure Communication Services. The API sends as its managed identity, so the
# service's connection strings and keys are never handed to anything.
#
# The Azure-managed domain (DoNotReply@<guid>.azurecomm.net) needs no DNS, but has low sending
# limits and an unfamiliar sender. Before a real pilot, a customer-managed domain is added in two
# applies (docs/runbooks/email-domain.md): first `custom_domain`, whose DNS records are output to
# add and verify; then `send_from_custom_domain`, once Azure shows the domain verified.

resource "azurerm_email_communication_service" "this" {
  name                = var.email_service_name
  resource_group_name = var.resource_group_name
  data_location       = var.data_location
  tags                = var.tags
}

resource "azurerm_email_communication_service_domain" "azure_managed" {
  name              = "AzureManagedDomain"
  email_service_id  = azurerm_email_communication_service.this.id
  domain_management = "AzureManaged"
  tags              = var.tags
}

resource "azurerm_communication_service" "this" {
  name                = var.communication_service_name
  resource_group_name = var.resource_group_name
  data_location       = var.data_location
  tags                = var.tags
}

resource "azurerm_communication_service_email_domain_association" "this" {
  communication_service_id = azurerm_communication_service.this.id
  email_service_domain_id  = azurerm_email_communication_service_domain.azure_managed.id
}

resource "azurerm_email_communication_service_domain" "custom" {
  count             = var.custom_domain == null ? 0 : 1
  name              = var.custom_domain
  email_service_id  = azurerm_email_communication_service.this.id
  domain_management = "CustomerManaged"
  tags              = var.tags
}

# Linked only once the domain is verified: ACS refuses to link an unverified one.
resource "azurerm_communication_service_email_domain_association" "custom" {
  count                    = var.send_from_custom_domain ? 1 : 0
  communication_service_id = azurerm_communication_service.this.id
  email_service_domain_id  = azurerm_email_communication_service_domain.custom[0].id
}
