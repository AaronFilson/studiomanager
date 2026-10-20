data "azurerm_client_config" "current" {}

data "azuread_client_config" "current" {}

locals {
  # Storage account names are global, so derive a short stable suffix from the subscription.
  suffix = substr(sha1(var.subscription_id), 0, 6)

  tags = {
    app          = "studiomanager"
    env          = "shared"
    "managed-by" = "terraform"
  }

  github_oidc_issuer   = "https://token.actions.githubusercontent.com"
  github_oidc_audience = ["api://AzureADTokenExchange"]

  # Roles the deploy identity may hand out inside an environment resource group, and only to
  # service principals (managed identities). Anything else, like Owner, is refused by Azure.
  assignable_role_ids = [
    "ba92f5b4-2d11-453d-a403-e96b0029c9fe", # Storage Blob Data Contributor
    "4633458b-17de-408a-b874-0445c86b69e6", # Key Vault Secrets User
    "3913510d-42f4-4e42-8a64-420c390055eb", # Monitoring Metrics Publisher
  ]
  assignable_role_list = join(", ", local.assignable_role_ids)

  # "Constrain roles" condition for Role Based Access Control Administrator, in the format at
  # https://learn.microsoft.com/azure/role-based-access-control/delegate-role-assignments-examples
  rbac_admin_condition = <<-EOT
    (
     (
      !(ActionMatches{'Microsoft.Authorization/roleAssignments/write'})
     )
     OR
     (
      @Request[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {${local.assignable_role_list}}
      AND
      @Request[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {'ServicePrincipal'}
     )
    )
    AND
    (
     (
      !(ActionMatches{'Microsoft.Authorization/roleAssignments/delete'})
     )
     OR
     (
      @Resource[Microsoft.Authorization/roleAssignments:RoleDefinitionId] ForAnyOfAnyValues:GuidEquals {${local.assignable_role_list}}
      AND
      @Resource[Microsoft.Authorization/roleAssignments:PrincipalType] ForAnyOfAnyValues:StringEqualsIgnoreCase {'ServicePrincipal'}
     )
    )
  EOT
}

# --- Shared resources: remote state and the GitHub identities -----------------------------

resource "azurerm_resource_group" "shared" {
  name     = "rg-studiomanager-shared"
  location = var.location
  tags     = local.tags
}

resource "azurerm_storage_account" "tfstate" {
  name                     = "sttfstudiomgr${local.suffix}"
  resource_group_name      = azurerm_resource_group.shared.name
  location                 = azurerm_resource_group.shared.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  min_tls_version          = "TLS1_2"

  # Entra ID only: no account keys, no SAS. The backend uses use_azuread_auth = true.
  shared_access_key_enabled       = false
  default_to_oauth_authentication = true
  allow_nested_items_to_be_public = false

  blob_properties {
    versioning_enabled = true

    delete_retention_policy {
      days = 30
    }

    container_delete_retention_policy {
      days = 30
    }
  }

  tags = local.tags

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_storage_container" "tfstate" {
  name                  = "tfstate"
  storage_account_id    = azurerm_storage_account.tfstate.id
  container_access_type = "private"

  lifecycle {
    prevent_destroy = true
  }
}

# Applies from main, behind the GitHub "dev" environment's protection rules.
resource "azurerm_user_assigned_identity" "github_deploy" {
  name                = "id-studiomanager-github-deploy"
  resource_group_name = azurerm_resource_group.shared.name
  location            = azurerm_resource_group.shared.location
  tags                = local.tags
}

# Read-only: plans on pull requests. Any branch can trigger a PR workflow, so the identity
# those workflows can reach must not be able to change anything.
resource "azurerm_user_assigned_identity" "github_plan" {
  name                = "id-studiomanager-github-plan"
  resource_group_name = azurerm_resource_group.shared.name
  location            = azurerm_resource_group.shared.location
  tags                = local.tags
}

resource "azurerm_federated_identity_credential" "deploy_environment" {
  for_each = var.environments

  name                      = "github-environment-${each.key}"
  user_assigned_identity_id = azurerm_user_assigned_identity.github_deploy.id
  issuer                    = local.github_oidc_issuer
  audience                  = local.github_oidc_audience
  subject                   = "repo:${var.github_repository}:environment:${each.key}"
}

resource "azurerm_federated_identity_credential" "plan_pull_request" {
  name                      = "github-pull-request"
  user_assigned_identity_id = azurerm_user_assigned_identity.github_plan.id
  issuer                    = local.github_oidc_issuer
  audience                  = local.github_oidc_audience
  subject                   = "repo:${var.github_repository}:pull_request"
}

resource "azurerm_federated_identity_credential" "plan_main" {
  name                      = "github-main"
  user_assigned_identity_id = azurerm_user_assigned_identity.github_plan.id
  issuer                    = local.github_oidc_issuer
  audience                  = local.github_oidc_audience
  subject                   = "repo:${var.github_repository}:ref:refs/heads/main"
}

resource "azurerm_role_assignment" "deploy_state" {
  scope                = azurerm_storage_container.tfstate.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_user_assigned_identity.github_deploy.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "plan_state" {
  scope                = azurerm_storage_container.tfstate.id
  role_definition_name = "Storage Blob Data Reader"
  principal_id         = azurerm_user_assigned_identity.github_plan.principal_id
  principal_type       = "ServicePrincipal"
}

# --- Per environment: the resource group, CI access to it, and a database admin group -----

# Created here, not in envs/<env>, so the CI identities can be scoped to it.
resource "azurerm_resource_group" "env" {
  for_each = var.environments

  name     = "rg-studiomanager-${each.key}"
  location = var.location
  tags     = merge(local.tags, { env = each.key })
}

resource "azurerm_role_assignment" "deploy_contributor" {
  for_each = var.environments

  scope                = azurerm_resource_group.env[each.key].id
  role_definition_name = "Contributor"
  principal_id         = azurerm_user_assigned_identity.github_deploy.principal_id
  principal_type       = "ServicePrincipal"
}

resource "azurerm_role_assignment" "deploy_rbac_admin" {
  for_each = var.environments

  scope                = azurerm_resource_group.env[each.key].id
  role_definition_name = "Role Based Access Control Administrator"
  principal_id         = azurerm_user_assigned_identity.github_deploy.principal_id
  principal_type       = "ServicePrincipal"
  condition_version    = "2.0"
  condition            = local.rbac_admin_condition
  description          = "Terraform may grant data-plane roles to the app's managed identities, and nothing else."
}

resource "azurerm_role_assignment" "plan_reader" {
  for_each = var.environments

  scope                = azurerm_resource_group.env[each.key].id
  role_definition_name = "Reader"
  principal_id         = azurerm_user_assigned_identity.github_plan.principal_id
  principal_type       = "ServicePrincipal"
}

# A second Microsoft Entra admin on each Postgres server, for people (the first is the
# migrator identity). Members sign in to psql with the group name as the user name.
resource "azuread_group" "db_admins" {
  for_each = var.environments

  display_name     = "studiomanager-${each.key}-db-admins"
  description      = "Microsoft Entra admins of the studiomanager ${each.key} Postgres server."
  security_enabled = true
  owners           = [data.azuread_client_config.current.object_id]
  members          = [data.azuread_client_config.current.object_id]
}
