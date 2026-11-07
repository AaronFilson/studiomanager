# Offline: mocked provider, no Azure credentials. Run with `terraform test` in modules/email.

mock_provider "azurerm" {}

variables {
  communication_service_name = "acs-studiomanager-test-abc123"
  email_service_name         = "ecs-studiomanager-test"
  resource_group_name        = "rg-studiomanager-test"
  data_location              = "United States"
  tags                       = { app = "studiomanager" }
}

run "azure_managed_only_by_default" {
  command = plan

  assert {
    condition     = length(azurerm_email_communication_service_domain.custom) == 0 && length(azurerm_communication_service_email_domain_association.custom) == 0
    error_message = "No custom domain until one is chosen."
  }
}

run "a_custom_domain_is_added_but_not_sent_from_until_verified" {
  command = plan

  variables {
    custom_domain = "mail.example.com"
  }

  assert {
    condition     = azurerm_email_communication_service_domain.custom[0].domain_management == "CustomerManaged"
    error_message = "The chosen domain is customer-managed, verified by DNS."
  }

  assert {
    condition     = length(azurerm_communication_service_email_domain_association.custom) == 0
    error_message = "ACS refuses to link a domain before it's verified, so the first apply doesn't link it."
  }
}

run "sending_from_the_custom_domain_links_it" {
  command = plan

  variables {
    custom_domain           = "mail.example.com"
    send_from_custom_domain = true
  }

  assert {
    condition     = length(azurerm_communication_service_email_domain_association.custom) == 1
    error_message = "The verified domain is linked to the Communication Services resource."
  }
}

run "sending_from_a_custom_domain_needs_one" {
  command = plan

  variables {
    send_from_custom_domain = true
  }

  expect_failures = [var.send_from_custom_domain]
}
