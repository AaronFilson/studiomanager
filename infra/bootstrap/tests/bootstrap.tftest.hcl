# Offline: mocked providers, no credentials. Run with `terraform test` in bootstrap.

mock_provider "azurerm" {
  mock_data "azurerm_client_config" {
    defaults = {
      tenant_id = "99999999-9999-9999-9999-999999999999"
    }
  }

  # The provider still validates IDs that flow between resources, so they must look real.
  mock_resource "azurerm_resource_group" {
    defaults = {
      id = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-mock"
    }
  }

  mock_resource "azurerm_storage_account" {
    defaults = {
      id = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-mock/providers/Microsoft.Storage/storageAccounts/stmock"
    }
  }

  mock_resource "azurerm_user_assigned_identity" {
    defaults = {
      id           = "/subscriptions/88888888-8888-8888-8888-888888888888/resourceGroups/rg-mock/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-mock"
      principal_id = "ffffffff-ffff-4fff-8fff-ffffffffffff"
    }
  }
}

mock_provider "azuread" {
  mock_data "azuread_client_config" {
    defaults = {
      object_id = "77777777-7777-4777-8777-777777777777"
    }
  }
}

variables {
  subscription_id = "88888888-8888-8888-8888-888888888888"
}

run "federation_and_scopes" {
  command = plan

  assert {
    condition     = azurerm_federated_identity_credential.deploy_environment["dev"].subject == "repo:AaronFilson/studiomanager:environment:dev"
    error_message = "The deploy identity must trust only the protected GitHub environment."
  }

  assert {
    condition = toset([
      azurerm_federated_identity_credential.plan_pull_request.subject,
      azurerm_federated_identity_credential.plan_main.subject,
      ]) == toset([
      "repo:AaronFilson/studiomanager:pull_request",
      "repo:AaronFilson/studiomanager:ref:refs/heads/main",
    ])
    error_message = "Pull requests and main may only reach the read-only plan identity."
  }

  assert {
    condition     = azurerm_role_assignment.deploy_rbac_admin["dev"].condition_version == "2.0" && strcontains(azurerm_role_assignment.deploy_rbac_admin["dev"].condition, "ba92f5b4-2d11-453d-a403-e96b0029c9fe, 4633458b-17de-408a-b874-0445c86b69e6, 3913510d-42f4-4e42-8a64-420c390055eb")
    error_message = "The deploy identity's role-assignment rights must be constrained to the allow-list."
  }

  assert {
    condition     = azurerm_role_assignment.plan_reader["dev"].role_definition_name == "Reader" && azurerm_role_assignment.plan_state.role_definition_name == "Storage Blob Data Reader"
    error_message = "The plan identity must stay read-only."
  }

  assert {
    condition     = azurerm_storage_account.tfstate.shared_access_key_enabled == false && length(azurerm_storage_account.tfstate.name) <= 24
    error_message = "State storage is Entra-only and its name fits Azure's limit."
  }
}
