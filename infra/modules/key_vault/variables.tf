variable "name" {
  description = "Globally unique, 3-24 characters."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "tenant_id" {
  type = string
}

variable "purge_protection_enabled" {
  description = "Off in dev so the vault can be torn down and recreated; turn it on for production."
  type        = bool
  default     = false
}

variable "tags" {
  type = map(string)
}
