# The GitHub sign-in bridge (ADR 20): a small OpenID Connect provider that Entra External ID
# federates, which signs people in with GitHub's OAuth. Its own Container App and its own identity,
# which can read only its secrets' vault. One replica at most: authorization codes and sign-in
# sessions are in memory (ADR 20).

locals {
  port     = 8080
  app_name = "ca-${var.name}-auth"
  issuer   = "https://${local.app_name}.${var.container_app_environment_default_domain}"

  # Container App secret name => the Key Vault secret it reads and the env var it becomes. The
  # values never pass through Terraform: the owner puts them in the vault (the runbook).
  vault_secrets = {
    "signing-keys"         = { vault_secret = "github-bridge-signing-keys", env = "STUDIOMANAGER_BRIDGE_SIGNING_KEYS" }
    "github-client-secret" = { vault_secret = "github-bridge-github-client-secret", env = "STUDIOMANAGER_BRIDGE_GITHUB_CLIENT_SECRET" }
    "entra-client-secret"  = { vault_secret = "github-bridge-entra-client-secret", env = "STUDIOMANAGER_BRIDGE_ENTRA_CLIENT_SECRET" }
  }

  env = {
    STUDIOMANAGER_BRIDGE_ISSUER              = local.issuer
    STUDIOMANAGER_BRIDGE_GITHUB_CLIENT_ID    = var.github_client_id
    STUDIOMANAGER_BRIDGE_ENTRA_CLIENT_ID     = var.entra_client_id
    STUDIOMANAGER_BRIDGE_ENTRA_REDIRECT_URIS = join(",", var.entra_redirect_uris)
  }
}

resource "azurerm_user_assigned_identity" "bridge" {
  name                = "id-${var.name}-auth"
  resource_group_name = var.resource_group_name
  location            = var.location
  tags                = var.tags
}

# principal_type is required by the deploy identity's role-assignment condition (infra/bootstrap).
resource "azurerm_role_assignment" "bridge_key_vault" {
  scope                            = var.key_vault_id
  role_definition_name             = "Key Vault Secrets User"
  principal_id                     = azurerm_user_assigned_identity.bridge.principal_id
  principal_type                   = "ServicePrincipal"
  skip_service_principal_aad_check = true
}

resource "azurerm_container_app" "bridge" {
  name                         = local.app_name
  resource_group_name          = var.resource_group_name
  container_app_environment_id = var.container_app_environment_id
  revision_mode                = "Single"
  tags                         = var.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [azurerm_user_assigned_identity.bridge.id]
  }

  # Versionless references: a new secret version reaches the app on its next revision or restart.
  dynamic "secret" {
    for_each = local.vault_secrets

    content {
      name                = secret.key
      key_vault_secret_id = "${var.key_vault_uri}secrets/${secret.value.vault_secret}"
      identity            = azurerm_user_assigned_identity.bridge.id
    }
  }

  ingress {
    external_enabled           = true
    target_port                = local.port
    transport                  = "auto"
    allow_insecure_connections = false

    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }

  template {
    min_replicas = 0
    max_replicas = 1

    container {
      name   = "auth"
      image  = var.image
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = local.env

        content {
          name  = env.key
          value = env.value
        }
      }

      dynamic "env" {
        for_each = local.vault_secrets

        content {
          name        = env.value.env
          secret_name = env.key
        }
      }

      startup_probe {
        transport               = "HTTP"
        port                    = local.port
        path                    = "/actuator/health/liveness"
        initial_delay           = 5
        interval_seconds        = 10
        timeout                 = 3
        failure_count_threshold = 10
      }

      liveness_probe {
        transport               = "HTTP"
        port                    = local.port
        path                    = "/actuator/health/liveness"
        interval_seconds        = 15
        timeout                 = 3
        failure_count_threshold = 3
      }

      readiness_probe {
        transport               = "HTTP"
        port                    = local.port
        path                    = "/actuator/health/readiness"
        interval_seconds        = 10
        timeout                 = 3
        failure_count_threshold = 3
      }
    }
  }

  # The identity must be able to read the vault before the app resolves its secrets.
  depends_on = [azurerm_role_assignment.bridge_key_vault]

  lifecycle {
    # CI deploys new images with `az containerapp update`.
    ignore_changes = [template[0].container[0].image]
  }
}
