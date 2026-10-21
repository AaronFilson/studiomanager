data "azurerm_client_config" "current" {}

# Created by infra/bootstrap, so the CI identities' rights can be scoped to it.
data "azurerm_resource_group" "env" {
  name = "rg-studiomanager-${var.environment}"
}

locals {
  name = "studiomanager-${var.environment}"

  # Short, stable suffix for globally unique names.
  suffix = substr(sha1("${data.azurerm_client_config.current.subscription_id}-${var.environment}"), 0, 6)

  tags = {
    app          = "studiomanager"
    env          = var.environment
    "managed-by" = "terraform"
  }

  web_origin = "https://${azurerm_static_web_app.web.default_host_name}"
  issuer_uri = "https://${var.entra_external_tenant_id}.ciamlogin.com/${var.entra_external_tenant_id}/v2.0"
}

# --- Identities ---------------------------------------------------------------------------
# User-assigned rather than system-assigned so they exist before the app does: Postgres needs
# the migrator as an admin, and role assignments need the API's principal, before first deploy.

resource "azurerm_user_assigned_identity" "api" {
  name                = "id-${local.name}-api"
  resource_group_name = data.azurerm_resource_group.env.name
  location            = var.location
  tags                = local.tags
}

resource "azurerm_user_assigned_identity" "migrator" {
  name                = "id-${local.name}-migrator"
  resource_group_name = data.azurerm_resource_group.env.name
  location            = var.location
  tags                = local.tags
}

# --- Data and platform --------------------------------------------------------------------

module "platform" {
  source = "../../modules/platform"

  name                = local.name
  resource_group_name = data.azurerm_resource_group.env.name
  location            = var.location
  tags                = local.tags
}

module "postgres" {
  source = "../../modules/postgres"

  server_name         = "psql-${local.name}-${local.suffix}"
  resource_group_name = data.azurerm_resource_group.env.name
  location            = var.location
  tenant_id           = data.azurerm_client_config.current.tenant_id
  migrator_identity = {
    principal_id = azurerm_user_assigned_identity.migrator.principal_id
    name         = azurerm_user_assigned_identity.migrator.name
  }
  admin_group = var.db_admin_group
  tags        = local.tags
}

module "storage" {
  source = "../../modules/storage"

  account_name         = "ststudiomgr${var.environment}${local.suffix}"
  resource_group_name  = data.azurerm_resource_group.env.name
  location             = var.location
  containers           = ["piece-photos"]
  cors_allowed_origins = [local.web_origin]
  tags                 = local.tags
}

module "email" {
  source = "../../modules/email"

  communication_service_name = "acs-${local.name}-${local.suffix}"
  email_service_name         = "ecs-${local.name}"
  resource_group_name        = data.azurerm_resource_group.env.name
  data_location              = var.communication_data_location
  tags                       = local.tags
}

module "key_vault" {
  source = "../../modules/key_vault"

  name                = "kv-studiomgr-${var.environment}-${local.suffix}"
  resource_group_name = data.azurerm_resource_group.env.name
  location            = var.location
  tenant_id           = data.azurerm_client_config.current.tenant_id
  tags                = local.tags
}

# --- Web and API --------------------------------------------------------------------------

resource "azurerm_static_web_app" "web" {
  name                = "stapp-${local.name}"
  resource_group_name = data.azurerm_resource_group.env.name
  location            = var.static_web_app_location
  sku_tier            = "Free"
  sku_size            = "Free"
  tags                = local.tags
}

module "api" {
  source = "../../modules/api"

  name                         = local.name
  resource_group_name          = data.azurerm_resource_group.env.name
  location                     = var.location
  container_app_environment_id = module.platform.container_app_environment_id
  image                        = var.api_image
  min_replicas                 = var.api_min_replicas

  api_identity = {
    id        = azurerm_user_assigned_identity.api.id
    client_id = azurerm_user_assigned_identity.api.client_id
    name      = azurerm_user_assigned_identity.api.name
  }
  migrator_identity = {
    id        = azurerm_user_assigned_identity.migrator.id
    client_id = azurerm_user_assigned_identity.migrator.client_id
    name      = azurerm_user_assigned_identity.migrator.name
  }

  db_host                                = module.postgres.fqdn
  db_name                                = module.postgres.database_name
  auth_issuer_uri                        = local.issuer_uri
  auth_audience                          = var.entra_api_client_id
  cors_allowed_origins                   = [local.web_origin]
  blob_endpoint                          = module.storage.primary_blob_endpoint
  web_origin                             = local.web_origin
  mail_acs_endpoint                      = module.email.endpoint
  mail_from                              = module.email.sender_address
  application_insights_connection_string = module.platform.application_insights_connection_string
  tags                                   = local.tags
}

# --- Data-plane access for the API identity -----------------------------------------------
# principal_type must be set: the deploy identity's role-assignment rights are conditioned on
# it (see infra/bootstrap), and the condition can't match an assignment that omits it.

resource "azurerm_role_assignment" "api_blob" {
  scope                            = module.storage.id
  role_definition_name             = "Storage Blob Data Contributor"
  principal_id                     = azurerm_user_assigned_identity.api.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

resource "azurerm_role_assignment" "api_key_vault" {
  scope                            = module.key_vault.id
  role_definition_name             = "Key Vault Secrets User"
  principal_id                     = azurerm_user_assigned_identity.api.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

# TODO(first deploy): the API identity needs a role on module.email.communication_service_id
# to send. Which one is unconfirmed: Microsoft's built-in role list has no Communication
# Services entry, and reports conflict on whether a custom role works. Find it with
# `az role definition list --query "[?contains(roleName, 'Communication')]"`, assign it here,
# and add its ID to assignable_role_ids in infra/bootstrap. See docs/adr/0011.
