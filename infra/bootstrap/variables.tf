variable "subscription_id" {
  description = "The Azure subscription that hosts every environment."
  type        = string
}

variable "location" {
  description = "Region for the shared resources and the environment resource groups."
  type        = string
  default     = "eastus2"
}

variable "github_repository" {
  description = "owner/name of the GitHub repository whose workflows may sign in. Case-sensitive: it must match the OIDC subject claim."
  type        = string
  default     = "AaronFilson/studiomanager"
}

variable "environments" {
  description = "Environments to prepare. Each gets a resource group, a GitHub environment federation and a database admin group."
  type        = set(string)
  default     = ["dev"]
}
