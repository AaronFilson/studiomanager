variable "name" {
  description = "Base name, e.g. studiomanager-dev."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "container_app_environment_id" {
  type = string
}

variable "image" {
  description = "Image used when the app and job are first created. After that CI rolls images with the Azure CLI, after migrations, so Terraform ignores this attribute."
  type        = string
}

variable "api_identity" {
  description = "User-assigned identity the API runs as. Its name is also its Postgres role."
  type = object({
    id        = string
    client_id = string
    name      = string
  })
}

variable "migrator_identity" {
  description = "User-assigned identity the migration job runs as (a Postgres Entra admin)."
  type = object({
    id        = string
    client_id = string
    name      = string
  })
}

variable "db_host" {
  type = string
}

variable "db_name" {
  type = string
}

variable "auth_issuer_uri" {
  type = string
}

variable "auth_audience" {
  type = string
}

variable "cors_allowed_origins" {
  type = list(string)
}

variable "blob_endpoint" {
  description = "Blob endpoint for piece photos (read by the API from M1 on)."
  type        = string
}

variable "web_origin" {
  description = "The web app's origin, for links in emails."
  type        = string
}

variable "mail_acs_endpoint" {
  description = "Communication Services endpoint the API sends email through."
  type        = string
}

variable "mail_from" {
  description = "Sender address on a domain linked to that Communication Services resource."
  type        = string
}

variable "application_insights_connection_string" {
  type      = string
  sensitive = true
}

variable "webhook_secret_key" {
  description = "Base64 AES-256 key that encrypts webhook signing secrets at rest (studiomanager.webhooks.secret-key)."
  type        = string
  sensitive   = true
}

variable "ai_key_encryption_key" {
  description = "Base64 AES-256 key that encrypts studios' AI provider keys at rest (studiomanager.ai.key-encryption-key)."
  type        = string
  sensitive   = true
}

variable "min_replicas" {
  description = "0 lets the API scale to zero when idle; the first request then pays a cold start."
  type        = number
  default     = 0
}

variable "max_replicas" {
  type    = number
  default = 2
}

variable "cpu" {
  type    = number
  default = 0.5
}

variable "memory" {
  type    = string
  default = "1Gi"
}

variable "trace_sampling_ratio" {
  description = "Share of traces kept (OpenTelemetry's parent-based trace-id ratio sampler), so the log quota is a backstop rather than the thing that cuts logs off at midday."
  type        = number
  default     = 0.1

  validation {
    condition     = var.trace_sampling_ratio >= 0 && var.trace_sampling_ratio <= 1
    error_message = "trace_sampling_ratio is a share, from 0 to 1."
  }
}

variable "sweep_cron_expression" {
  description = "When the sweep job runs every sweep once, in UTC (ADR 23). Every 6 hours by default."
  type        = string
  default     = "0 */6 * * *"
}

variable "tags" {
  type = map(string)
}
