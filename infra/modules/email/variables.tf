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

variable "tags" {
  type = map(string)
}
