# Offline: mocked provider, no Azure credentials. Run with `terraform test` in
# modules/document_intelligence.

mock_provider "azurerm" {}

variables {
  name                = "di-studiomanager-test-abc123"
  resource_group_name = "rg-studiomanager-test"
  location            = "westus"
  tags                = { app = "studiomanager" }
}

run "free_tier_with_entra_sign_in_only" {
  command = plan

  assert {
    condition     = azurerm_cognitive_account.this.kind == "FormRecognizer" && azurerm_cognitive_account.this.sku_name == "F0"
    error_message = "Document Intelligence on the free F0 tier by default."
  }

  assert {
    condition     = azurerm_cognitive_account.this.local_auth_enabled == false
    error_message = "No keys: the API signs in as its managed identity."
  }

  assert {
    condition     = azurerm_cognitive_account.this.custom_subdomain_name == var.name
    error_message = "Entra sign-in needs a custom subdomain."
  }

  assert {
    condition     = output.endpoint == "https://di-studiomanager-test-abc123.cognitiveservices.azure.com"
    error_message = "The endpoint is the custom subdomain's."
  }
}

run "rejects_other_tiers" {
  command = plan

  variables {
    sku_name = "S1"
  }

  expect_failures = [var.sku_name]
}
