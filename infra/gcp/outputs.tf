output "api_env" {
  description = "Paste into infra/envs/dev/dev.tfvars as google_invoice_reader."
  value = {
    location                   = var.document_ai_location
    processor_name             = google_document_ai_processor.invoice.id
    workload_identity_audience = "//iam.googleapis.com/${google_iam_workload_identity_pool_provider.api.name}"
    service_account_email      = google_service_account.invoice_reader.email
    azure_token_scope          = "${var.azure_token_audience}/.default"
  }
}
