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
