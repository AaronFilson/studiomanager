# Offline: mocked provider, no Google credentials. Run with
# `terraform init -backend=false && terraform test` in infra/gcp.

mock_provider "google" {
  mock_resource "google_iam_workload_identity_pool" {
    defaults = {
      name = "projects/123456789012/locations/global/workloadIdentityPools/studiomanager-dev-azure"
    }
  }

  mock_resource "google_iam_workload_identity_pool_provider" {
    defaults = {
      name = "projects/123456789012/locations/global/workloadIdentityPools/studiomanager-dev-azure/providers/api-managed-identity"
    }
  }

  mock_resource "google_service_account" {
    defaults = {
      email = "studiomanager-dev-invoices@studiomanager-dev-000000.iam.gserviceaccount.com"
      name  = "projects/studiomanager-dev-000000/serviceAccounts/studiomanager-dev-invoices@studiomanager-dev-000000.iam.gserviceaccount.com"
    }
  }

  mock_resource "google_document_ai_processor" {
    defaults = {
      id = "projects/studiomanager-dev-000000/locations/us/processors/abc123"
    }
  }
}

variables {
  project_id       = "studiomanager-dev-000000"
  azure_tenant_id  = "99999999-9999-9999-9999-999999999999"
  api_principal_id = "ffffffff-ffff-4fff-8fff-ffffffffffff"
}

run "only_the_api_identity_is_trusted" {
  command = apply # mocked: computed values are generated, nothing is created

  assert {
    condition     = google_iam_workload_identity_pool_provider.api.attribute_condition == "assertion.sub == 'ffffffff-ffff-4fff-8fff-ffffffffffff'"
    error_message = "Only the API managed identity's tokens may be exchanged."
  }

  assert {
    condition     = google_iam_workload_identity_pool_provider.api.oidc[0].issuer_uri == "https://login.microsoftonline.com/99999999-9999-9999-9999-999999999999/v2.0"
    error_message = "The provider trusts the Azure tenant's issuer."
  }

  assert {
    condition     = google_service_account_iam_member.api_impersonates_invoice_reader.member == "principal://iam.googleapis.com/projects/123456789012/locations/global/workloadIdentityPools/studiomanager-dev-azure/subject/ffffffff-ffff-4fff-8fff-ffffffffffff"
    error_message = "Only the API identity may impersonate the service account."
  }

  assert {
    condition     = google_project_iam_member.invoice_reader_document_ai.role == "roles/documentai.apiUser"
    error_message = "The service account may call Document AI, and nothing more."
  }

  assert {
    condition     = google_document_ai_processor.invoice.type == "INVOICE_PROCESSOR"
    error_message = "The processor is the Invoice Parser."
  }

  assert {
    condition = output.api_env == {
      location                   = "us"
      processor_name             = "projects/studiomanager-dev-000000/locations/us/processors/abc123"
      workload_identity_audience = "//iam.googleapis.com/projects/123456789012/locations/global/workloadIdentityPools/studiomanager-dev-azure/providers/api-managed-identity"
      service_account_email      = "studiomanager-dev-invoices@studiomanager-dev-000000.iam.gserviceaccount.com"
      azure_token_scope          = "api://AzureADTokenExchange/.default"
    }
    error_message = "The API's settings must match what GoogleReaderProperties reads."
  }
}

run "rejects_a_project_id_that_is_not_one" {
  command = plan

  variables {
    project_id = "My Project"
  }

  expect_failures = [var.project_id]
}
