# Offline: mocked provider, no Azure credentials, no backend. Run with
# `terraform init -backend=false && terraform test` in envs/dev.

mock_provider "azurerm" {
  mock_data "azurerm_client_config" {
    defaults = {
      tenant_id       = "99999999-9999-9999-9999-999999999999"
      subscription_id = "88888888-8888-8888-8888-888888888888"
    }
  }

  mock_data "azurerm_resource_group" {
    defaults = {
      id       = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev"
      location = "eastus2"
    }
  }

  mock_resource "azurerm_static_web_app" {
    defaults = {
      default_host_name = "happy-rock-0a1b2c3d4.azurestaticapps.net"
    }
  }

  # The provider still validates IDs that flow between resources, so they must look real.
  mock_resource "azurerm_user_assigned_identity" {
    defaults = {
      id           = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-mock"
      client_id    = "eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee"
      principal_id = "ffffffff-ffff-4fff-8fff-ffffffffffff"
    }
  }

  mock_resource "azurerm_log_analytics_workspace" {
    defaults = {
      id = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.OperationalInsights/workspaces/log-mock"
    }
  }

  mock_resource "azurerm_container_app_environment" {
    defaults = {
      id = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.App/managedEnvironments/cae-mock"
    }
  }

  mock_resource "azurerm_postgresql_flexible_server" {
    defaults = {
      id   = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.DBforPostgreSQL/flexibleServers/psql-mock"
      fqdn = "psql-mock.postgres.database.azure.com"
    }
  }

  mock_resource "azurerm_storage_account" {
    defaults = {
      id                    = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.Storage/storageAccounts/stmock"
      primary_blob_endpoint = "https://stmock.blob.core.windows.net/"
    }
  }

  mock_resource "azurerm_key_vault" {
    defaults = {
      id = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.KeyVault/vaults/kv-mock"
    }
  }

  mock_resource "azurerm_email_communication_service" {
    defaults = {
      id = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.Communication/emailServices/ecs-mock"
    }
  }

  mock_resource "azurerm_email_communication_service_domain" {
    defaults = {
      id                 = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.Communication/emailServices/ecs-mock/domains/AzureManagedDomain"
      from_sender_domain = "00000000-0000-0000-0000-000000000000.azurecomm.net"
    }
  }

  mock_resource "azurerm_communication_service" {
    defaults = {
      id       = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-studiomanager-dev/providers/Microsoft.Communication/communicationServices/acs-mock"
      hostname = "acs-mock.unitedstates.communication.azure.com"
    }
  }
}

variables {
  environment                     = "dev"
  entra_external_tenant_id        = "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
  entra_external_tenant_subdomain = "studiomanagerdev"
  entra_api_client_id             = "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
  entra_web_client_id             = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
  db_admin_group = {
    object_id    = "dddddddd-dddd-4ddd-8ddd-dddddddddddd"
    display_name = "studiomanager-dev-db-admins"
  }
}

run "wires_the_environment" {
  command = apply # mocked: computed values are generated, nothing is created

  assert {
    condition = output.web_build_env == {
      VITE_ENTRA_CLIENT_ID = "cccccccc-cccc-4ccc-8ccc-cccccccccccc"
      VITE_ENTRA_AUTHORITY = "https://studiomanagerdev.ciamlogin.com/"
      VITE_API_SCOPES      = "api://bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb/access_as_user"
      VITE_API_BASE_URL    = output.api_url
    }
    error_message = "Web build settings must match the web app's contract."
  }

  assert {
    condition     = output.api_identity_name == "id-studiomanager-dev-api" && output.migrator_identity_name == "id-studiomanager-dev-migrator"
    error_message = "Identity names are also Postgres role names; keep them stable."
  }

  assert {
    condition     = module.storage.name == "ststudiomgrdev${substr(sha1("88888888-8888-8888-8888-888888888888-dev"), 0, 6)}" && length(module.storage.name) <= 24
    error_message = "Storage account name must be stable and at most 24 characters."
  }

  assert {
    condition     = length(module.key_vault.name) <= 24
    error_message = "Key Vault names are limited to 24 characters."
  }

  assert {
    condition     = module.email.endpoint == "https://acs-mock.unitedstates.communication.azure.com" && module.email.sender_address == "DoNotReply@00000000-0000-0000-0000-000000000000.azurecomm.net"
    error_message = "The API sends from the Azure-managed domain through the Communication Services endpoint."
  }

  assert {
    condition     = azurerm_role_assignment.api_blob.principal_type == "ServicePrincipal" && azurerm_role_assignment.api_key_vault.principal_type == "ServicePrincipal"
    error_message = "principal_type is required by the deploy identity's role-assignment condition."
  }
}

run "rejects_unfilled_placeholders" {
  command = plan

  variables {
    entra_external_tenant_id = "REPLACE_ME"
    entra_api_client_id      = "REPLACE_ME"
    entra_web_client_id      = "REPLACE_ME"
    db_admin_group = {
      object_id    = "REPLACE_ME"
      display_name = "studiomanager-dev-db-admins"
    }
  }

  expect_failures = [
    var.entra_external_tenant_id,
    var.entra_api_client_id,
    var.entra_web_client_id,
    var.db_admin_group,
  ]
}
