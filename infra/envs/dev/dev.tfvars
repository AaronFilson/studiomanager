# Non-secret settings for dev. Replace the REPLACE_ME values once (see
# docs/runbooks/first-deploy.md); validation fails the plan until you do.

environment             = "dev"
# West US, the subscription's region. Static Web Apps isn't offered there, and it serves from a
# global edge anyway. The resource group stays in eastus2 (bootstrap); that's only its metadata.
location                = "westus"
static_web_app_location = "eastus2"

# From `terraform output env_tfvars` in infra/identity
entra_external_tenant_id        = "694cd29a-7b3a-4b47-90a0-465a3c1e0482"
entra_external_tenant_subdomain = "studiomanagerdev"
entra_api_client_id             = "8ce4649e-bbda-42c8-96b9-8f8d9d360be4"
entra_web_client_id             = "51589e60-343d-4ff9-b287-63a708b2ff1d"

# From `terraform output db_admin_groups` in infra/bootstrap
db_admin_group = {
  object_id    = "764d9ae5-48af-44be-a54a-b0e03e4133a9"
  display_name = "studiomanager-dev-db-admins"
}
