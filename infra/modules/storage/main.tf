# Blob storage for uploads. Entra ID only: no account keys, so the API hands out
# user-delegation SAS tokens, which are signed with its managed identity.

resource "azurerm_storage_account" "this" {
  name                     = var.account_name
  resource_group_name      = var.resource_group_name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  shared_access_key_enabled       = false
  default_to_oauth_authentication = true
  allow_nested_items_to_be_public = false

  blob_properties {
    delete_retention_policy {
      days = 7
    }

    container_delete_retention_policy {
      days = 7
    }

    dynamic "cors_rule" {
      for_each = length(var.cors_allowed_origins) > 0 ? [1] : []

      content {
        allowed_origins    = var.cors_allowed_origins
        allowed_methods    = ["GET", "HEAD", "PUT", "OPTIONS"]
        allowed_headers    = ["*"]
        exposed_headers    = ["etag", "x-ms-*"]
        max_age_in_seconds = 3600
      }
    }
  }

  tags = var.tags

  lifecycle {
    # Holds members' photos: a change that would replace the account fails the plan instead.
    prevent_destroy = true
  }
}

resource "azurerm_storage_container" "this" {
  for_each = var.containers

  name                  = each.key
  storage_account_id    = azurerm_storage_account.this.id
  container_access_type = "private"
}
