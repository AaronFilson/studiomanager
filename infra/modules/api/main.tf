# The API Container App and the Flyway migration job. Both run the same image as different
# identities: the app's identity can only read and write rows; the job's identity owns the schema.

locals {
  port = 8080

  # Settings the app and the job share. The job overrides who it connects as.
  common_env = {
    SPRING_PROFILES_ACTIVE              = "azure"
    STUDIOMANAGER_DB_HOST               = var.db_host
    STUDIOMANAGER_DB_NAME               = var.db_name
    STUDIOMANAGER_DB_USER               = var.api_identity.name
    AZURE_CLIENT_ID                     = var.api_identity.client_id
    STUDIOMANAGER_AUTH_ISSUER_URI       = var.auth_issuer_uri
    STUDIOMANAGER_AUTH_AUDIENCE         = var.auth_audience
    STUDIOMANAGER_CORS_ALLOWED_ORIGINS  = join(",", var.cors_allowed_origins)
    STUDIOMANAGER_STORAGE_BLOB_ENDPOINT = var.blob_endpoint
  }

  migrate_env = merge(local.common_env, {
    SPRING_PROFILES_ACTIVE         = "azure,migrate"
    STUDIOMANAGER_DB_USER          = var.migrator_identity.name
    AZURE_CLIENT_ID                = var.migrator_identity.client_id
    STUDIOMANAGER_DB_API_PRINCIPAL = var.api_identity.name
  })

  app_insights_secret = "appinsights-connection-string"
}

resource "azurerm_container_app" "api" {
  name                         = "ca-${var.name}-api"
  resource_group_name          = var.resource_group_name
  container_app_environment_id = var.container_app_environment_id
  revision_mode                = "Single"
  tags                         = var.tags

  identity {
    type         = "UserAssigned"
    identity_ids = [var.api_identity.id]
  }

  # The image is public on GHCR, so there is no registry block and no pull secret.

  secret {
    name  = local.app_insights_secret
    value = var.application_insights_connection_string
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
    min_replicas = var.min_replicas
    max_replicas = var.max_replicas

    http_scale_rule {
      name                = "http"
      concurrent_requests = "50"
    }

    container {
      name   = "api"
      image  = var.image
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = local.common_env

        content {
          name  = env.key
          value = env.value
        }
      }

      env {
        name        = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        secret_name = local.app_insights_secret
      }

      # The JVM gets up to ~2 minutes to start before liveness takes over.
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

  lifecycle {
    # CI deploys new images with `az containerapp update`, only after the migration job succeeds.
    ignore_changes = [template[0].container[0].image]
  }
}

resource "azurerm_container_app_job" "migrate" {
  name                         = "caj-${var.name}-migrate"
  resource_group_name          = var.resource_group_name
  location                     = var.location
  container_app_environment_id = var.container_app_environment_id
  tags                         = var.tags

  # Flyway runs each migration in a transaction, so a failure leaves nothing half-applied.
  # Fail fast instead of retrying something deterministic.
  replica_timeout_in_seconds = 600
  replica_retry_limit        = 0

  manual_trigger_config {
    parallelism              = 1
    replica_completion_count = 1
  }

  identity {
    type         = "UserAssigned"
    identity_ids = [var.migrator_identity.id]
  }

  template {
    container {
      name   = "migrate"
      image  = var.image
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = local.migrate_env

        content {
          name  = env.key
          value = env.value
        }
      }
    }
  }

  lifecycle {
    ignore_changes = [template[0].container[0].image]
  }
}
