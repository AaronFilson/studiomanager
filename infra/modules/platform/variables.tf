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

variable "log_retention_days" {
  type    = number
  default = 30
}

variable "log_daily_quota_gb" {
  description = "Log Analytics daily ingestion cap, so a noisy environment can't run up a bill. 0.15 GB a day stays inside the free 5 GB a month; trace sampling keeps normal days well under it. -1 means no cap."
  type        = number
  default     = 0.15
}

variable "tags" {
  type = map(string)
}
