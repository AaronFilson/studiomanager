# terraform init -backend-config=backend.hcl -backend-config=storage_account_name=<TFSTATE_STORAGE_ACCOUNT>
resource_group_name = "rg-studiomanager-shared"
container_name      = "tfstate"
key                 = "dev.tfstate"
use_azuread_auth    = true
