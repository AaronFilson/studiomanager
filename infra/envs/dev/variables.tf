variable "environment" {
  description = "Short environment name. Kept short because it is part of globally unique names with tight length limits (Key Vault: 24 characters)."
  type        = string

  validation {
    condition     = can(regex("^[a-z]{2,5}$", var.environment))
    error_message = "Use 2-5 lowercase letters, e.g. dev or prod."
  }
}

variable "location" {
  type    = string
  default = "eastus2"
}

variable "static_web_app_location" {
  description = "Static Web Apps is offered in fewer regions (for example westus2, centralus, eastus2, westeurope, eastasia)."
  type        = string
  default     = "eastus2"
}

variable "api_image" {
  description = "Image used only when the Container App and job are first created. CI rolls later images after migrations."
  type        = string
  default     = "ghcr.io/aaronfilson/studiomanager-api:main"
}

variable "entra_external_tenant_id" {
  description = "From `terraform output env_tfvars` in infra/identity."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$", var.entra_external_tenant_id))
    error_message = "Must be a GUID. Copy it from the infra/identity outputs."
  }
}

variable "entra_external_tenant_subdomain" {
  description = "From `terraform output env_tfvars` in infra/identity."
  type        = string

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.entra_external_tenant_subdomain))
    error_message = "Must be the tenant's domain prefix. Copy it from the infra/identity outputs."
  }
}

variable "entra_api_client_id" {
  description = "From `terraform output env_tfvars` in infra/identity."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$", var.entra_api_client_id))
    error_message = "Must be a GUID. Copy it from the infra/identity outputs."
  }
}

variable "entra_web_client_id" {
  description = "From `terraform output env_tfvars` in infra/identity."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$", var.entra_web_client_id))
    error_message = "Must be a GUID. Copy it from the infra/identity outputs."
  }
}

variable "db_admin_group" {
  description = "From `terraform output db_admin_groups` in infra/bootstrap: the people who are Postgres Entra admins."
  type = object({
    object_id    = string
    display_name = string
  })

  validation {
    condition     = can(regex("^[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$", var.db_admin_group.object_id))
    error_message = "object_id must be a GUID. Copy it from the infra/bootstrap outputs."
  }
}

variable "api_min_replicas" {
  type    = number
  default = 0
}
