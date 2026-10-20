# PostgreSQL Flexible Server with Microsoft Entra authentication only: no passwords exist.
#
# Flexible Server accepts one control-plane operation at a time and rejects the rest with
# "ServerBusy", so the child resources below are chained with depends_on rather than created
# in parallel.

resource "azurerm_postgresql_flexible_server" "this" {
  name                = var.server_name
  resource_group_name = var.resource_group_name
  location            = var.location
  version             = var.postgres_version
  sku_name            = var.sku_name
  storage_mb          = var.storage_mb

  backup_retention_days        = var.backup_retention_days
  geo_redundant_backup_enabled = false
  auto_grow_enabled            = true

  # Public endpoint plus a firewall for now; see infra/README.md for the private-network plan.
  public_network_access_enabled = true

  authentication {
    active_directory_auth_enabled = true
    password_auth_enabled         = false
    tenant_id                     = var.tenant_id
  }

  tags = var.tags

  lifecycle {
    # Holds studio data: a change that would replace the server fails the plan instead.
    # Tearing an environment down is a deliberate code change.
    prevent_destroy = true

    # Azure picks an availability zone when none is given; don't fight it on later plans.
    ignore_changes = [zone]
  }
}

resource "azurerm_postgresql_flexible_server_database" "app" {
  name      = var.database_name
  server_id = azurerm_postgresql_flexible_server.this.id
  charset   = "UTF8"
  collation = "en_US.utf8"

  lifecycle {
    prevent_destroy = true
  }
}

# 0.0.0.0 is Azure's convention for "any Azure service", which covers Container Apps on the
# Consumption plan (their outbound IPs aren't fixed). Entra auth is still required.
resource "azurerm_postgresql_flexible_server_firewall_rule" "azure_services" {
  name             = "AllowAzureServices"
  server_id        = azurerm_postgresql_flexible_server.this.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"

  depends_on = [azurerm_postgresql_flexible_server_database.app]
}

resource "azurerm_postgresql_flexible_server_active_directory_administrator" "migrator" {
  server_name         = azurerm_postgresql_flexible_server.this.name
  resource_group_name = var.resource_group_name
  tenant_id           = var.tenant_id
  object_id           = var.migrator_identity.principal_id
  principal_name      = var.migrator_identity.name
  principal_type      = "ServicePrincipal"

  depends_on = [azurerm_postgresql_flexible_server_firewall_rule.azure_services]
}

resource "azurerm_postgresql_flexible_server_active_directory_administrator" "admin_group" {
  count = var.admin_group == null ? 0 : 1

  server_name         = azurerm_postgresql_flexible_server.this.name
  resource_group_name = var.resource_group_name
  tenant_id           = var.tenant_id
  object_id           = var.admin_group.object_id
  principal_name      = var.admin_group.display_name
  principal_type      = "Group"

  depends_on = [azurerm_postgresql_flexible_server_active_directory_administrator.migrator]
}
