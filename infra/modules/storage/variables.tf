variable "account_name" {
  description = "Globally unique, 3-24 lowercase letters and digits."
  type        = string
}

variable "resource_group_name" {
  type = string
}

variable "location" {
  type = string
}

variable "containers" {
  description = "Private blob containers to create."
  type        = set(string)
}

variable "cors_allowed_origins" {
  description = "Browser origins allowed to PUT and GET blobs directly with a user-delegation SAS (piece photos are uploaded straight from the client)."
  type        = list(string)
  default     = []
}

variable "tags" {
  type = map(string)
}
