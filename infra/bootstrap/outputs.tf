output "github_variables" {
  description = "Repository variables to set in GitHub (Settings > Secrets and variables > Actions > Variables). None of them is a secret."
  value = {
    AZURE_TENANT_ID         = data.azurerm_client_config.current.tenant_id
    AZURE_SUBSCRIPTION_ID   = var.subscription_id
    AZURE_CLIENT_ID_DEPLOY  = azurerm_user_assigned_identity.github_deploy.client_id
    AZURE_CLIENT_ID_PLAN    = azurerm_user_assigned_identity.github_plan.client_id
    TFSTATE_STORAGE_ACCOUNT = azurerm_storage_account.tfstate.name
  }
}

output "terraform_init_command" {
  description = "How to initialise an environment root locally against the remote state."
  value       = "terraform init -backend-config=backend.hcl -backend-config=storage_account_name=${azurerm_storage_account.tfstate.name}"
}

output "db_admin_groups" {
  description = "Paste into envs/<env>/<env>.tfvars as db_admin_group."
  value = {
    for env, group in azuread_group.db_admins : env => {
      object_id    = group.object_id
      display_name = group.display_name
    }
  }
}

output "environment_resource_groups" {
  value = { for env, rg in azurerm_resource_group.env : env => rg.name }
}
