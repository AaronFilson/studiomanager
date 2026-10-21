# App registrations in the Microsoft Entra External ID (customer) tenant. Applied by a person
# signed in to that tenant (`az login --tenant <external-tenant-id> --allow-no-subscriptions`).
# CI never touches it: the CI identities live in the workforce tenant and have no rights here.
#
# State is local (git-ignored). It holds no secrets, and the registrations can be re-imported
# if it is lost. See ../README.md.
terraform {
  required_version = "~> 1.16"

  required_providers {
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.10"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.9"
    }
  }
}

provider "azuread" {
  tenant_id = var.external_tenant_id
}
