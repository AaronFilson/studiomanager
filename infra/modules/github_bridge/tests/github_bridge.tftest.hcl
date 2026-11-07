# Offline: mocked provider, no Azure credentials. Run with `terraform test` in modules/github_bridge.

mock_provider "azurerm" {
  mock_resource "azurerm_user_assigned_identity" {
    defaults = {
      id           = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-studiomanager-test/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-studiomanager-test-auth"
      principal_id = "55555555-5555-4555-8555-555555555555"
    }
  }
}

variables {
  name                                     = "studiomanager-test"
  resource_group_name                      = "rg-studiomanager-test"
  location                                 = "eastus2"
  container_app_environment_id             = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-studiomanager-test/providers/Microsoft.App/managedEnvironments/cae-studiomanager-test"
  container_app_environment_default_domain = "happy-sea-123.westus.azurecontainerapps.io"
  image                                    = "ghcr.io/aaronfilson/studiomanager-auth:abc123"
  key_vault_id                             = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-studiomanager-test/providers/Microsoft.KeyVault/vaults/kv-test"
  key_vault_uri                            = "https://kv-test.vault.azure.net/"
  github_client_id                         = "Ov23liTestClientId"
  entra_client_id                          = "studiomanager-entra"
  entra_redirect_uris                      = ["https://tenant.ciamlogin.com/aaaa/federation/oauth2", "https://tenant.ciamlogin.com/tenant.onmicrosoft.com/federation/oauth2"]
  tags                                     = { app = "studiomanager" }
}

run "secrets_come_from_the_vault_and_settings_from_plain_env" {
  command = plan

  assert {
    condition = alltrue([
      for name, vault_secret in {
        "signing-keys"         = "https://kv-test.vault.azure.net/secrets/github-bridge-signing-keys"
        "github-client-secret" = "https://kv-test.vault.azure.net/secrets/github-bridge-github-client-secret"
        "entra-client-secret"  = "https://kv-test.vault.azure.net/secrets/github-bridge-entra-client-secret"
      } : anytrue([for s in azurerm_container_app.bridge.secret : s.name == name && s.key_vault_secret_id == vault_secret])
    ])
    error_message = "Each of the bridge's secrets is a Key Vault reference, never a value in state."
  }

  assert {
    condition = alltrue([
      for name, secret in {
        STUDIOMANAGER_BRIDGE_SIGNING_KEYS         = "signing-keys"
        STUDIOMANAGER_BRIDGE_GITHUB_CLIENT_SECRET = "github-client-secret"
        STUDIOMANAGER_BRIDGE_ENTRA_CLIENT_SECRET  = "entra-client-secret"
      } : anytrue([for e in azurerm_container_app.bridge.template[0].container[0].env : e.name == name && e.secret_name == secret])
    ])
    error_message = "Secret env vars must come from Container App secrets."
  }

  assert {
    condition = alltrue([
      for name, expected in {
        STUDIOMANAGER_BRIDGE_ISSUER              = "https://ca-studiomanager-test-auth.happy-sea-123.westus.azurecontainerapps.io"
        STUDIOMANAGER_BRIDGE_GITHUB_CLIENT_ID    = "Ov23liTestClientId"
        STUDIOMANAGER_BRIDGE_ENTRA_CLIENT_ID     = "studiomanager-entra"
        STUDIOMANAGER_BRIDGE_ENTRA_REDIRECT_URIS = "https://tenant.ciamlogin.com/aaaa/federation/oauth2,https://tenant.ciamlogin.com/tenant.onmicrosoft.com/federation/oauth2"
      } : { for e in azurerm_container_app.bridge.template[0].container[0].env : e.name => e.value }[name] == expected
    ])
    error_message = "The bridge's settings don't match its application.yaml."
  }

  assert {
    condition     = output.issuer == "https://ca-studiomanager-test-auth.happy-sea-123.westus.azurecontainerapps.io"
    error_message = "The issuer is the app's own external host."
  }
}

run "one_replica_at_most_because_codes_are_in_memory" {
  command = plan

  assert {
    condition     = azurerm_container_app.bridge.template[0].max_replicas == 1 && azurerm_container_app.bridge.template[0].min_replicas == 0
    error_message = "ADR 20: codes and sessions are in memory, so the bridge must never run two replicas."
  }

  assert {
    condition     = azurerm_container_app.bridge.ingress[0].allow_insecure_connections == false
    error_message = "The bridge is served over https only."
  }

  assert {
    condition     = azurerm_role_assignment.bridge_key_vault.role_definition_name == "Key Vault Secrets User" && azurerm_role_assignment.bridge_key_vault.principal_type == "ServicePrincipal"
    error_message = "The bridge's identity reads secrets only, and principal_type is set for the deploy identity's condition."
  }
}

run "rejects_a_vault_uri_without_its_slash" {
  command = plan

  variables {
    key_vault_uri = "https://kv-test.vault.azure.net"
  }

  expect_failures = [var.key_vault_uri]
}
