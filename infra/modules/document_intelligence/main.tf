# Azure AI Document Intelligence, for reading supplier invoices with the prebuilt invoice model
# (docs/planning/20, "Invoice readers"). The API calls it as its managed identity, so local
# (key) auth is off and the account needs a custom subdomain, which Entra sign-in requires.
#
# F0 is free: 500 pages a month, only the first two pages of each document, 4 MB per file. A
# subscription may have one F0 account of this kind.

resource "azurerm_cognitive_account" "this" {
  name                  = var.name
  resource_group_name   = var.resource_group_name
  location              = var.location
  kind                  = "FormRecognizer"
  sku_name              = var.sku_name
  custom_subdomain_name = var.name
  local_auth_enabled    = false
  tags                  = var.tags
}
