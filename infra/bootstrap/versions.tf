# Applied once, by a person, with local state (it creates the remote state storage, so it
# cannot live there on its first apply). See ../README.md.
terraform {
  required_version = "~> 1.16"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.10"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 3.10"
    }
  }
}

provider "azurerm" {
  features {}

  subscription_id     = var.subscription_id
  storage_use_azuread = true

  # azurerm 5 registers nothing by default. Bootstrap runs as a subscription Owner, so it
  # registers what the environments need; CI identities are scoped to resource groups and
  # could not do this themselves.
  resource_providers_to_register = [
    "Microsoft.AlertsManagement",
    "Microsoft.App",
    "Microsoft.DBforPostgreSQL",
    "Microsoft.Insights",
    "Microsoft.KeyVault",
    "Microsoft.ManagedIdentity",
    "Microsoft.OperationalInsights",
    "Microsoft.Storage",
    "Microsoft.Web",
  ]
}

# The workforce tenant that owns the subscription (not the External ID tenant).
provider "azuread" {}
