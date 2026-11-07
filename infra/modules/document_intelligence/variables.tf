variable "name" {
  description = "Globally unique; also the endpoint's subdomain."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "sku_name" {
  description = "F0 is free (500 pages a month); S0 is paid by the page."
  type        = string
  default     = "F0"

  validation {
    condition     = contains(["F0", "S0"], var.sku_name)
    error_message = "Use F0 or S0."
  }
}

variable "tags" {
  type = map(string)
}
