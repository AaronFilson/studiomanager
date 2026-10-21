variable "external_tenant_id" {
  description = "Tenant ID of the Microsoft Entra External ID tenant."
  type        = string
}

variable "external_tenant_subdomain" {
  description = "The tenant's initial domain prefix: `studiomanagerdev` for studiomanagerdev.onmicrosoft.com. Sign-in happens at https://<subdomain>.ciamlogin.com/."
  type        = string
}

variable "environment" {
  description = "Environment name, used in display names."
  type        = string
  default     = "dev"
}

variable "mobile_redirect_uri" {
  description = "Where sign-in returns to the mobile app: its URL scheme (app.config.ts) plus /auth."
  type        = string
  default     = "studiomanager://auth"
}

variable "web_origins" {
  description = "Origins the SPA is served from, without a trailing slash; each is registered as the redirect URI `<origin>/`. Add the Static Web App origin (https://<default-host-name>) after the first envs/<env> apply."
  type        = list(string)
  default     = ["http://localhost:5173"]

  validation {
    condition     = alltrue([for o in var.web_origins : can(regex("^https?://[^/]+$", o))])
    error_message = "Each origin is scheme://host[:port] with no path or trailing slash."
  }
}
