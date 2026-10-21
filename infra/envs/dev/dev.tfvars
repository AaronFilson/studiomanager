# Non-secret settings for dev. Replace the REPLACE_ME values once (see
# docs/runbooks/first-deploy.md); validation fails the plan until you do.

environment             = "dev"
location                = "eastus2"
static_web_app_location = "eastus2"

# From `terraform output env_tfvars` in infra/identity
entra_external_tenant_id        = "REPLACE_ME"
entra_external_tenant_subdomain = "REPLACE_ME"
entra_api_client_id             = "REPLACE_ME"
entra_web_client_id             = "REPLACE_ME"

# From `terraform output db_admin_groups` in infra/bootstrap
db_admin_group = {
  object_id    = "REPLACE_ME"
  display_name = "studiomanager-dev-db-admins"
}
