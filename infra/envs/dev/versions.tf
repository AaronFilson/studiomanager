terraform {
  required_version = "~> 1.16"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 5.10"
    }
  }

  # Partial configuration: see backend.hcl. The storage account name comes from the
  # TFSTATE_STORAGE_ACCOUNT GitHub variable (or the bootstrap output when run locally).
  backend "azurerm" {}
}

# Credentials and the subscription come from the environment: ARM_SUBSCRIPTION_ID plus
# ARM_USE_OIDC/ARM_CLIENT_ID/ARM_TENANT_ID in CI, or the Azure CLI locally.
provider "azurerm" {
  features {}

  # Storage accounts here have shared keys disabled.
  storage_use_azuread = true
}
