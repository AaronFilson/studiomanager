variable "subscription_id" {
  description = "The Azure subscription that hosts every environment."
  type        = string
}

variable "location" {
  description = "Region for the shared resources and the environment resource groups. Pick the one nearest the people running the service."
  type        = string
  default     = "westus"
}

variable "github_oidc_subject_prefix" {
  description = "Start of the subject claim in GitHub's OIDC tokens for this repository. GitHub's immutable form names the owner and repository by name and numeric id, so a rename can't hand the trust to someone else. Read it with `gh api repos/<owner>/<repo>/actions/oidc/customization/sub` (sub_claim_prefix)."
  type        = string
  default     = "repo:AaronFilson@14099435/studiomanager@1426092900"
}

variable "environments" {
  description = "Environments to prepare. Each gets a resource group, a GitHub environment federation and a database admin group."
  type        = set(string)
  default     = ["dev"]
}
