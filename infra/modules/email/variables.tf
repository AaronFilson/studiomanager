variable "communication_service_name" {
  description = "Globally unique; also the endpoint's host name."
  type        = string
}

variable "email_service_name" {
  type = string
}

variable "resource_group_name" {
  type = string
}

variable "data_location" {
  description = "Where ACS keeps data at rest, such as \"United States\". Both services are global; this is not an Azure region."
  type        = string
}

variable "custom_domain" {
  description = "A domain the studio platform sends from, such as \"mail.example.com\". Null keeps only the Azure-managed domain."
  type        = string
  default     = null
}

variable "send_from_custom_domain" {
  description = "Send from custom_domain. Turn on only after Azure shows the domain verified."
  type        = bool
  default     = false

  validation {
    condition     = !var.send_from_custom_domain || var.custom_domain != null
    error_message = "send_from_custom_domain needs custom_domain."
  }
}

variable "tags" {
  type = map(string)
}
