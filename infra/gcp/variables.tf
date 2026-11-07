variable "project_id" {
  description = "The Google Cloud project, with billing linked (docs/runbooks/google-document-ai.md)."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$", var.project_id))
    error_message = "Must be a project id: 6-30 lowercase letters, digits and hyphens."
  }
}

variable "environment" {
  type    = string
  default = "dev"

  validation {
    condition     = can(regex("^[a-z]{2,5}$", var.environment))
    error_message = "Use 2-5 lowercase letters, e.g. dev or prod."
  }
}

variable "document_ai_location" {
  description = "Where the processor runs and keeps data: us or eu."
  type        = string
  default     = "us"

  validation {
    condition     = contains(["us", "eu"], var.document_ai_location)
    error_message = "Document AI's Invoice Parser runs in us or eu."
  }
}

variable "azure_tenant_id" {
  description = "The Azure subscription's (workforce) tenant, which issues the API managed identity's tokens. Not the External ID tenant."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$", var.azure_tenant_id))
    error_message = "Must be a GUID: `az account show --query tenantId -o tsv`."
  }
}

variable "api_principal_id" {
  description = "The API managed identity's principal (object) id: `terraform output api_identity_principal_id` in infra/envs/dev. Only tokens for it are accepted."
  type        = string

  validation {
    condition     = can(regex("^[0-9a-f]{8}-([0-9a-f]{4}-){3}[0-9a-f]{12}$", var.api_principal_id))
    error_message = "Must be a GUID."
  }
}

variable "azure_token_audience" {
  description = "The audience of the managed-identity token the API sends. The API asks for \"<this>/.default\"."
  type        = string
  default     = "api://AzureADTokenExchange"
}

variable "azure_token_issuer" {
  description = "The managed-identity token's `iss`. Empty means Entra's v2 issuer for the tenant. Check it against a real token on first use (docs/runbooks/google-document-ai.md)."
  type        = string
  default     = ""
}
