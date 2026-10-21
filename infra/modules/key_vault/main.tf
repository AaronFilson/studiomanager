# Empty for now; secrets that can't be avoided later (payment-provider and QuickBooks OAuth tokens, for example) go here.
# RBAC mode means access is granted with role assignments, the same as everything else.

resource "azurerm_key_vault" "this" {
  name                       = var.name
  resource_group_name        = var.resource_group_name
  location                   = var.location
  tenant_id                  = var.tenant_id
  sku_name                   = "standard"
  rbac_authorization_enabled = true
  soft_delete_retention_days = 7
  purge_protection_enabled   = var.purge_protection_enabled
  tags                       = var.tags

  lifecycle {
    prevent_destroy = true
  }
}
