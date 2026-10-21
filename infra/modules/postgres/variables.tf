variable "server_name" {
  description = "Globally unique server name (it becomes <name>.postgres.database.azure.com)."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "tenant_id" {
  description = "Workforce tenant used for Microsoft Entra authentication to the server."
  type        = string
}

variable "postgres_version" {
  type    = string
  default = "18"
}

variable "sku_name" {
  description = "B_Standard_B1ms is the free-account-eligible Burstable size."
  type        = string
  default     = "B_Standard_B1ms"
}

variable "storage_mb" {
  type    = number
  default = 32768
}

variable "backup_retention_days" {
  type    = number
  default = 7
}

variable "database_name" {
  type    = string
  default = "studiomanager"
}

variable "migrator_identity" {
  description = "Managed identity that runs Flyway. It becomes a Microsoft Entra admin, so it owns the schema."
  type = object({
    principal_id = string
    name         = string
  })
}

variable "admin_group" {
  description = "Optional Entra group of people who are also admins (for one-time setup and break-glass). Null to skip."
  type = object({
    object_id    = string
    display_name = string
  })
  default = null
}

variable "tags" {
  type = map(string)
}
