variable "name" {
  description = "Base name, e.g. studiomanager-dev. The app is ca-<name>-auth."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "container_app_environment_id" {
  type = string
}

variable "container_app_environment_default_domain" {
  description = "The environment's default domain: the bridge's issuer is https://ca-<name>-auth.<domain>."
  type        = string
}

variable "image" {
  description = "Image used when the app is first created. After that CI rolls images with the Azure CLI, so Terraform ignores this attribute."
  type        = string
}

variable "key_vault_id" {
  description = "The vault holding the bridge's three secrets (docs/runbooks/github-sign-in.md)."
  type        = string
}

variable "key_vault_uri" {
  description = "The vault's URI, with its trailing slash."
  type        = string

  validation {
    condition     = endswith(var.key_vault_uri, "/")
    error_message = "key_vault_uri ends with a slash, as azurerm_key_vault.vault_uri does."
  }
}

variable "github_client_id" {
  description = "The GitHub OAuth app's client ID (not a secret)."
  type        = string
}

variable "entra_client_id" {
  description = "The client ID Entra's custom OIDC identity provider presents to the bridge."
  type        = string
}

variable "entra_redirect_uris" {
  description = "Entra's federation callbacks, matched exactly by the bridge."
  type        = list(string)
}

variable "mcp_enabled" {
  description = "Enable the Phase 2 MCP OAuth 2.1 authorization server on the bridge (ADR 26)."
  type        = bool
  default     = false
}

variable "mcp_allowed_resources" {
  description = "Allowed RFC 8707 resource URIs or API origins for MCP access tokens."
  type        = list(string)
  default     = []

  validation {
    condition     = !var.mcp_enabled || length(var.mcp_allowed_resources) > 0
    error_message = "mcp_allowed_resources must contain at least one resource URI or API origin when mcp_enabled is true."
  }
}

variable "mcp_scopes" {
  description = "Comma-separated MCP OAuth scopes clients may request."
  type        = string
  default     = "read,propose"
}

variable "mcp_pre_registered_client_id" {
  description = "Optional pre-registered public PKCE MCP client ID (in addition to RFC 7591 dynamic client registration)."
  type        = string
  default     = ""
}

variable "mcp_pre_registered_redirect_uris" {
  description = "Redirect URIs for the optional pre-registered MCP client."
  type        = list(string)
  default     = []
}

variable "cpu" {
  type    = number
  default = 0.25
}

variable "memory" {
  type    = string
  default = "0.5Gi"
}

variable "tags" {
  type = map(string)
}
