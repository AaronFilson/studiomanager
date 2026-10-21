# Offline: mocked provider, no Azure credentials. Run with `terraform test` in modules/api.

mock_provider "azurerm" {}

variables {
  name                         = "studiomanager-test"
  resource_group_name          = "rg-studiomanager-test"
  location                     = "eastus2"
  container_app_environment_id = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-studiomanager-test/providers/Microsoft.App/managedEnvironments/cae-studiomanager-test"
  image                        = "ghcr.io/aaronfilson/studiomanager-api:abc123"

  api_identity = {
    id        = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-studiomanager-test/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-studiomanager-test-api"
    client_id = "11111111-1111-1111-1111-111111111111"
    name      = "id-studiomanager-test-api"
  }
  migrator_identity = {
    id        = "/subscriptions/00000000-0000-0000-0000-000000000000/resourceGroups/rg-studiomanager-test/providers/Microsoft.ManagedIdentity/userAssignedIdentities/id-studiomanager-test-migrator"
    client_id = "22222222-2222-2222-2222-222222222222"
    name      = "id-studiomanager-test-migrator"
  }

  db_host                                = "psql-studiomanager-test.postgres.database.azure.com"
  db_name                                = "studiomanager"
  auth_issuer_uri                        = "https://33333333-3333-3333-3333-333333333333.ciamlogin.com/33333333-3333-3333-3333-333333333333/v2.0"
  auth_audience                          = "44444444-4444-4444-4444-444444444444"
  cors_allowed_origins                   = ["https://a.azurestaticapps.net", "https://b.example"]
  blob_endpoint                          = "https://ststudiomgrtest.blob.core.windows.net/"
  web_origin                             = "https://a.azurestaticapps.net"
  mail_acs_endpoint                      = "https://acs-studiomanager-test.unitedstates.communication.azure.com"
  mail_from                              = "DoNotReply@00000000-0000-0000-0000-000000000000.azurecomm.net"
  application_insights_connection_string = "InstrumentationKey=00000000-0000-0000-0000-000000000000"
  tags                                   = { app = "studiomanager" }
}

run "api_runs_as_the_api_identity" {
  command = plan

  assert {
    condition     = azurerm_container_app.api.identity[0].identity_ids == toset([var.api_identity.id])
    error_message = "The app must run as the API identity only."
  }

  assert {
    condition = alltrue([
      for name, expected in {
        SPRING_PROFILES_ACTIVE             = "azure"
        STUDIOMANAGER_DB_USER              = "id-studiomanager-test-api"
        AZURE_CLIENT_ID                    = "11111111-1111-1111-1111-111111111111"
        STUDIOMANAGER_DB_HOST              = "psql-studiomanager-test.postgres.database.azure.com"
        STUDIOMANAGER_DB_NAME              = "studiomanager"
        STUDIOMANAGER_AUTH_AUDIENCE        = "44444444-4444-4444-4444-444444444444"
        STUDIOMANAGER_CORS_ALLOWED_ORIGINS = "https://a.azurestaticapps.net,https://b.example"
        STUDIOMANAGER_WEB_ORIGIN           = "https://a.azurestaticapps.net"
        STUDIOMANAGER_MAIL_SENDER          = "acs"
        STUDIOMANAGER_MAIL_ACS_ENDPOINT    = "https://acs-studiomanager-test.unitedstates.communication.azure.com"
        STUDIOMANAGER_MAIL_FROM            = "DoNotReply@00000000-0000-0000-0000-000000000000.azurecomm.net"
      } : { for e in azurerm_container_app.api.template[0].container[0].env : e.name => e.value }[name] == expected
    ])
    error_message = "The app's environment doesn't match the API contract."
  }

  assert {
    condition = anytrue([
      for e in azurerm_container_app.api.template[0].container[0].env :
      e.name == "APPLICATIONINSIGHTS_CONNECTION_STRING" && e.secret_name == "appinsights-connection-string"
    ])
    error_message = "The App Insights connection string must come from a secret, not a plain env value."
  }

  assert {
    condition     = azurerm_container_app.api.ingress[0].target_port == 8080 && azurerm_container_app.api.template[0].min_replicas == 0
    error_message = "Ingress targets 8080 and the app scales to zero."
  }

  assert {
    condition = (
      azurerm_container_app.api.template[0].container[0].liveness_probe[0].path == "/actuator/health/liveness" &&
      azurerm_container_app.api.template[0].container[0].readiness_probe[0].path == "/actuator/health/readiness"
    )
    error_message = "Probes must use the actuator health groups."
  }
}

run "migration_job_runs_as_the_migrator" {
  command = plan

  assert {
    condition     = azurerm_container_app_job.migrate.identity[0].identity_ids == toset([var.migrator_identity.id])
    error_message = "The job must run as the migrator identity only."
  }

  assert {
    condition = alltrue([
      for name, expected in {
        SPRING_PROFILES_ACTIVE         = "azure,migrate"
        STUDIOMANAGER_DB_USER          = "id-studiomanager-test-migrator"
        AZURE_CLIENT_ID                = "22222222-2222-2222-2222-222222222222"
        STUDIOMANAGER_DB_API_PRINCIPAL = "id-studiomanager-test-api"
      } : { for e in azurerm_container_app_job.migrate.template[0].container[0].env : e.name => e.value }[name] == expected
    ])
    error_message = "The job's environment must override the profile and connect as the migrator."
  }

  assert {
    condition     = azurerm_container_app_job.migrate.replica_retry_limit == 0
    error_message = "Migrations fail fast rather than retrying."
  }
}
