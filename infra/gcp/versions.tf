# Google Cloud for the Document AI invoice reader (docs/planning/20, "Invoice readers"). Applied
# once by a person signed in to Google (`gcloud auth application-default login`), like bootstrap
# and identity. CI never touches it, so CI needs no Google identity.
#
# State is local (git-ignored). It holds no secrets: there is no service-account key, the API
# signs in by workload identity federation. See docs/runbooks/google-document-ai.md.
terraform {
  required_version = "~> 1.16"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 8.6"
    }
  }
}

provider "google" {
  project = var.project_id
}
