locals {
  name = "studiomanager-${var.environment}"

  issuer = var.azure_token_issuer != "" ? var.azure_token_issuer : "https://login.microsoftonline.com/${var.azure_tenant_id}/v2.0"

  services = [
    "documentai.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "sts.googleapis.com",
  ]
}

resource "google_project_service" "this" {
  for_each = toset(local.services)

  service = each.value

  # Turning an API off could break other things in the project; leave that to a person.
  disable_on_destroy = false
}

# --- The processor ------------------------------------------------------------------------

resource "google_document_ai_processor" "invoice" {
  location     = var.document_ai_location
  display_name = "${local.name}-invoices"
  type         = "INVOICE_PROCESSOR"

  depends_on = [google_project_service.this]
}

# --- Who may call it ----------------------------------------------------------------------
# The API's managed-identity token from Entra is exchanged for a Google token through this pool,
# and then impersonates the service account. Only the API identity's tokens are accepted, and
# no service-account key exists.

resource "google_iam_workload_identity_pool" "azure" {
  workload_identity_pool_id = "${local.name}-azure"
  display_name              = "Studio Manager ${var.environment} (Azure)"
  description               = "The Studio Manager API's Azure managed identity."

  depends_on = [google_project_service.this]
}

resource "google_iam_workload_identity_pool_provider" "api" {
  workload_identity_pool_id          = google_iam_workload_identity_pool.azure.workload_identity_pool_id
  workload_identity_pool_provider_id = "api-managed-identity"
  display_name                       = "API managed identity"

  attribute_mapping = {
    "google.subject" = "assertion.sub"
  }
  attribute_condition = "assertion.sub == '${var.api_principal_id}'"

  oidc {
    issuer_uri        = local.issuer
    allowed_audiences = [var.azure_token_audience]
  }
}

resource "google_service_account" "invoice_reader" {
  account_id   = "${local.name}-invoices"
  display_name = "Studio Manager ${var.environment} invoice reader"
  description  = "Calls Document AI for the Studio Manager API. Impersonated through workload identity federation; it has no keys."
}

resource "google_project_iam_member" "invoice_reader_document_ai" {
  project = var.project_id
  role    = "roles/documentai.apiUser"
  member  = "serviceAccount:${google_service_account.invoice_reader.email}"
}

resource "google_service_account_iam_member" "api_impersonates_invoice_reader" {
  service_account_id = google_service_account.invoice_reader.name
  role               = "roles/iam.workloadIdentityUser"
  member             = "principal://iam.googleapis.com/${google_iam_workload_identity_pool.azure.name}/subject/${var.api_principal_id}"
}
