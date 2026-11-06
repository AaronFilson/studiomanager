locals {
  graph_client_id = "00000003-0000-0000-c000-000000000000" # Microsoft Graph

  # The web app redirects to the origin root with a trailing slash, and Entra matches redirect
  # URIs exactly.
  spa_redirect_uris = [for origin in var.web_origins : "${origin}/"]
}

resource "random_uuid" "access_as_user_scope" {}

resource "random_uuid" "platform_admin_role" {}

data "azuread_service_principal" "graph" {
  client_id = local.graph_client_id
}

# --- The API (resource server) ------------------------------------------------------------

resource "azuread_application" "api" {
  display_name     = "studiomanager-${var.environment}-api"
  sign_in_audience = "AzureADMyOrg" # the only audience External ID tenants support

  api {
    # v2 tokens: `aud` is the client ID and `iss` is https://<tenant-id>.ciamlogin.com/<tenant-id>/v2.0
    requested_access_token_version = 2

    oauth2_permission_scope {
      id                         = random_uuid.access_as_user_scope.result
      value                      = "access_as_user"
      type                       = "User"
      enabled                    = true
      admin_consent_display_name = "Use Studio Manager as the signed-in user"
      admin_consent_description  = "Lets the Studio Manager apps call the Studio Manager API as the signed-in user."
      user_consent_display_name  = "Use Studio Manager"
      user_consent_description   = "Lets Studio Manager act on your behalf."
    }
  }

  # The people who run the service, not a studio role (ADR 24). It arrives in the access token's
  # `roles` claim, and the API requires it on /api/v1/admin/**.
  app_role {
    id                   = random_uuid.platform_admin_role.result
    value                = "Platform.Admin"
    allowed_member_types = ["User"]
    enabled              = true
    display_name         = "Platform admin"
    description          = "Runs the service: live rate limits, boosts and the settings history."
  }

  optional_claims {
    access_token {
      name = "email"
    }
  }

  lifecycle {
    # Set by azuread_application_identifier_uri.api: it needs this app's client ID.
    ignore_changes = [identifier_uris]
  }
}

resource "azuread_application_identifier_uri" "api" {
  application_id = azuread_application.api.id
  identifier_uri = "api://${azuread_application.api.client_id}"
}

resource "azuread_service_principal" "api" {
  client_id = azuread_application.api.client_id
}

# The owner's account, and anyone else listed, gets the platform admin role.
resource "azuread_app_role_assignment" "platform_admin" {
  for_each = toset(var.platform_admin_object_ids)

  app_role_id         = random_uuid.platform_admin_role.result
  principal_object_id = each.value
  resource_object_id  = azuread_service_principal.api.object_id
}

# --- The single-page app (public client, auth code + PKCE) --------------------------------

resource "azuread_application" "web" {
  display_name     = "studiomanager-${var.environment}-web"
  sign_in_audience = "AzureADMyOrg"

  single_page_application {
    redirect_uris = local.spa_redirect_uris
  }

  required_resource_access {
    resource_app_id = azuread_application.api.client_id

    resource_access {
      id   = random_uuid.access_as_user_scope.result
      type = "Scope"
    }
  }

  required_resource_access {
    resource_app_id = local.graph_client_id

    resource_access {
      id   = data.azuread_service_principal.graph.oauth2_permission_scope_ids["openid"]
      type = "Scope"
    }

    resource_access {
      id   = data.azuread_service_principal.graph.oauth2_permission_scope_ids["offline_access"]
      type = "Scope"
    }
  }
}

resource "azuread_service_principal" "web" {
  client_id = azuread_application.web.client_id
}

# Tenant-wide admin consent, so customers never see a consent prompt.
resource "azuread_service_principal_delegated_permission_grant" "web_to_api" {
  service_principal_object_id          = azuread_service_principal.web.object_id
  resource_service_principal_object_id = azuread_service_principal.api.object_id
  claim_values                         = ["access_as_user"]
}

resource "azuread_service_principal_delegated_permission_grant" "web_to_graph" {
  service_principal_object_id          = azuread_service_principal.web.object_id
  resource_service_principal_object_id = data.azuread_service_principal.graph.object_id
  claim_values                         = ["openid", "offline_access"]
}

# --- The mobile app (public client, auth code + PKCE, custom-scheme redirect) -------------
# A separate registration because a native app's redirect is a custom scheme, which the SPA
# platform doesn't allow, and so either app's client ID can be rotated without the other.

resource "azuread_application" "mobile" {
  display_name     = "studiomanager-${var.environment}-mobile"
  sign_in_audience = "AzureADMyOrg"

  public_client {
    redirect_uris = [var.mobile_redirect_uri]
  }

  required_resource_access {
    resource_app_id = azuread_application.api.client_id

    resource_access {
      id   = random_uuid.access_as_user_scope.result
      type = "Scope"
    }
  }

  required_resource_access {
    resource_app_id = local.graph_client_id

    resource_access {
      id   = data.azuread_service_principal.graph.oauth2_permission_scope_ids["openid"]
      type = "Scope"
    }

    resource_access {
      id   = data.azuread_service_principal.graph.oauth2_permission_scope_ids["offline_access"]
      type = "Scope"
    }
  }
}

resource "azuread_service_principal" "mobile" {
  client_id = azuread_application.mobile.client_id
}

resource "azuread_service_principal_delegated_permission_grant" "mobile_to_api" {
  service_principal_object_id          = azuread_service_principal.mobile.object_id
  resource_service_principal_object_id = azuread_service_principal.api.object_id
  claim_values                         = ["access_as_user"]
}

resource "azuread_service_principal_delegated_permission_grant" "mobile_to_graph" {
  service_principal_object_id          = azuread_service_principal.mobile.object_id
  resource_service_principal_object_id = data.azuread_service_principal.graph.object_id
  claim_values                         = ["openid", "offline_access"]
}
