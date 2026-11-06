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
    STUDIOMANAGER_WEB_ORIGIN            = var.web_origin
    STUDIOMANAGER_MAIL_SENDER           = "acs"
    STUDIOMANAGER_MAIL_ACS_ENDPOINT     = var.mail_acs_endpoint
    STUDIOMANAGER_MAIL_FROM             = var.mail_from
    # Keep a share of traces, following the caller's decision when there is one (plan 20)
    OTEL_TRACES_SAMPLER     = "parentbased_traceidratio"
    OTEL_TRACES_SAMPLER_ARG = tostring(var.trace_sampling_ratio)
  }

  migrate_env = merge(local.common_env, {
    SPRING_PROFILES_ACTIVE         = "azure,migrate"
    STUDIOMANAGER_DB_USER          = var.migrator_identity.name
    AZURE_CLIENT_ID                = var.migrator_identity.client_id
    STUDIOMANAGER_DB_API_PRINCIPAL = var.api_identity.name
  })

  # The sweep job is the API without a web server: same identity, same settings (ADR 23)
  sweep_env = merge(local.common_env, {
    SPRING_PROFILES_ACTIVE = "azure,sweep"
  })

  app_insights_secret = "appinsights-connection-string"

  # Encryption keys the azure profile requires. The migration job starts the same Spring
  # context as the app, so it needs them too. Container App secret names map to env vars.
  encryption_secrets = {
    "webhook-secret-key"    = { env = "STUDIOMANAGER_WEBHOOK_SECRET_KEY", value = var.webhook_secret_key }
    "ai-key-encryption-key" = { env = "STUDIOMANAGER_AI_KEY_ENCRYPTION_KEY", value = var.ai_key_encryption_key }
  }
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

  dynamic "secret" {
    for_each = local.encryption_secrets

    content {
      name  = secret.key
      value = secret.value.value
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

      dynamic "env" {
        for_each = local.encryption_secrets

        content {
          name        = env.value.env
          secret_name = env.key
        }
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

  dynamic "secret" {
    for_each = local.encryption_secrets

    content {
      name  = secret.key
      value = secret.value.value
    }
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

      dynamic "env" {
        for_each = local.encryption_secrets

        content {
          name        = env.value.env
          secret_name = env.key
        }
      }
    }
  }

  lifecycle {
    ignore_changes = [template[0].container[0].image]
  }
}

# Runs every sweep once and exits, so retries, reminders and cleanups happen while the API is
# scaled to zero (ADR 23). An advisory lock per sweep keeps it from overlapping a replica that is
# up and running the same sweep on its own schedule.
resource "azurerm_container_app_job" "sweep" {
  name                         = "caj-${var.name}-sweep"
  resource_group_name          = var.resource_group_name
  location                     = var.location
  container_app_environment_id = var.container_app_environment_id
  tags                         = var.tags

  # A run takes about a minute. The next scheduled run is the retry, so a failure isn't repeated
  # straight away.
  replica_timeout_in_seconds = 900
  replica_retry_limit        = 0

  schedule_trigger_config {
    cron_expression          = var.sweep_cron_expression
    parallelism              = 1
    replica_completion_count = 1
  }

  identity {
    type         = "UserAssigned"
    identity_ids = [var.api_identity.id]
  }

  secret {
    name  = local.app_insights_secret
    value = var.application_insights_connection_string
  }

  template {
    container {
      name   = "sweep"
      image  = var.image
      cpu    = var.cpu
      memory = var.memory

      dynamic "env" {
        for_each = local.sweep_env

        content {
          name  = env.key
          value = env.value
        }
      }

      env {
        name        = "APPLICATIONINSIGHTS_CONNECTION_STRING"
        secret_name = local.app_insights_secret
      }
    }
  }

  lifecycle {
    # CI rolls the image along with the app's (see the deploy workflow)
    ignore_changes = [template[0].container[0].image]
  }
}
