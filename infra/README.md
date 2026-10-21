# Infrastructure

Terraform for Studio Manager on Azure. To go from an empty subscription to a running
"hello studio", follow [docs/runbooks/first-deploy.md](../docs/runbooks/first-deploy.md).

## Layout

```
infra/
  bootstrap/      once per subscription, by a person, local state
                  state storage, GitHub OIDC identities, env resource groups, DB admin groups
  identity/       once per environment, by a person signed in to the External ID tenant
                  app registrations: studiomanager-<env>-api and studiomanager-<env>-web
  envs/dev/       the dev environment; CI plans on PRs and applies on merge
    backend.hcl   remote-state settings (the storage account name comes from a GitHub variable)
    dev.tfvars    non-secret settings, committed
    tests/        `terraform test` with mocked providers (offline)
  modules/
    platform/     Log Analytics, Application Insights, Container Apps environment
    postgres/     Flexible Server (PostgreSQL 18), Entra-only auth
    api/          the API Container App and the Flyway migration job
    storage/      blob storage for piece photos
    key_vault/    Key Vault (RBAC mode)
  .tflint.hcl
```

### Apply order

| Step | Root | Who / how | State |
|---|---|---|---|
| 1 | `bootstrap` | You, as subscription Owner (`az login`) | local, git-ignored |
| 2 | `identity` | You, signed in to the External ID tenant | local, git-ignored |
| 3 | `envs/dev` | GitHub Actions (`deploy-dev.yml`) | Azure Storage, `dev.tfstate` |

Then, once per environment, there's one manual database step that Terraform can't do (step 12 of the runbook).

## What's in dev

| Resource | Name | Notes |
|---|---|---|
| Resource group | `rg-studiomanager-dev` | Created by bootstrap so CI's rights can be scoped to it |
| Managed identities | `id-studiomanager-dev-api`, `id-studiomanager-dev-migrator` | Each name is also the identity's Postgres role name |
| Container Apps env | `cae-studiomanager-dev` | Consumption plan, logs to Log Analytics |
| Container App | `ca-studiomanager-dev-api` | 0–2 replicas, port 8080, actuator probes |
| Container Apps Job | `caj-studiomanager-dev-migrate` | Manual trigger; same image, `azure,migrate` profile |
| PostgreSQL | `psql-studiomanager-dev-<hash>` | 18, B1ms, database `studiomanager`, Entra auth only |
| Storage | `ststudiomgrdev<hash>` | `piece-photos` container, no shared keys, CORS for the web origin |
| Key Vault | `kv-studiomgr-dev-<hash>` | RBAC mode, empty for now |
| Static Web App | `stapp-studiomanager-dev` | Free tier; serves `web/dist` |
| Monitoring | `log-studiomanager-dev`, `appi-studiomanager-dev` | 0.5 GB/day ingestion cap |

`<hash>` is the first 6 hex characters of `sha1("<subscription-id>-dev")`, so names are globally unique but stable.

Data-bearing resources (Postgres server and database, the storage account, Key Vault, and the state storage) have
`prevent_destroy`. A change that would replace one fails the plan instead of deleting data. Tearing
an environment down is therefore a deliberate code change.

## Decisions worth explaining

**No secrets in GitHub.** Workflows sign in to Azure with OIDC federated credentials on user-assigned
managed identities. Nothing to rotate, nothing to leak. There are two identities:

- `id-studiomanager-github-deploy` trusts only `environment:dev`, so it is usable only by jobs running in the
  GitHub `dev` environment. That environment is restricted to `main` and can require reviewers. It has
  Contributor on the env resource group, plus *Role Based Access Control Administrator* **with a condition**:
  it may only grant Storage Blob Data Contributor, Key Vault Secrets User and Monitoring Metrics Publisher,
  and only to service principals. So a compromised pipeline cannot make itself Owner.
- `id-studiomanager-github-plan` trusts `pull_request` and `refs/heads/main`. Any branch can open a PR, so
  this identity is read-only: Reader on the resource group, Storage Blob Data Reader on state. PR plans
  run with `-lock=false -refresh=false`. Refreshing would call list-secrets actions (Static Web Apps does)
  that Reader rightly lacks. The PR plan shows configuration changes, and the deploy job's refreshed
  plan catches drift.

**Passwordless Postgres.** Password auth is disabled on the server, so no admin password exists anywhere.
The API connects as its managed identity through the `azure-identity-extensions` JDBC plugin, and gets a
short-lived Entra token per connection.

**A separate migrator identity.** Flyway runs as a Container Apps Job under `id-…-migrator`, which is a
Postgres Entra admin and owns the schema. The API's identity has only the `app_rw` group role (DML),
so a bug or injection in the API cannot drop a table or alter row-level security policies. CI
runs migrations *before* rolling out the new image, so new code never runs against an old schema.

**The image is set once, then CI rolls it.** Terraform creates the app and job with the current
image and then ignores `image`. The deploy workflow migrates first, then calls `az containerapp update`.
This keeps "what infrastructure exists" (Terraform) separate from "which build is live" (the pipeline).

**Scale to zero.** Consumption-plan Container Apps with `min_replicas = 0` cost nothing when idle. The
trade-off is a JVM cold start on the first request. The startup probe allows about 2 minutes, and the
API image is built to start fast (see `api/Dockerfile`). Set `api_min_replicas = 1` when a pilot
studio is using it.

**GHCR rather than ACR.** The repo is public, so its GHCR package can be public too: free, no
registry to run, and Container Apps pulls anonymously, so there are no pull credentials at all. ACR Basic
(~$5/month) with an `AcrPull` role for the API identity is the drop-in alternative if the image ever needs to be
private. The deploy workflow pushes with the job's `GITHUB_TOKEN`.

**Resource groups are created in bootstrap.** That lets CI's rights be scoped to a resource group
rather than the subscription. Resource provider registration (`azurerm` 5 registers nothing by default) also
happens in bootstrap, because it needs subscription-level rights.

## Running Terraform locally

```bash
az login
export ARM_SUBSCRIPTION_ID=$(az account show --query id -o tsv)
cd infra/envs/dev
terraform init -backend-config=backend.hcl -backend-config=storage_account_name=<TFSTATE_STORAGE_ACCOUNT>
terraform plan -var-file=dev.tfvars
```

You need data access to the state container (Storage Blob Data Reader or Contributor) and rights on
`rg-studiomanager-dev`. As subscription Owner you can grant those to yourself. Prefer letting CI apply.

Offline checks (CI runs the same):

```bash
cd infra
terraform fmt -check -recursive
for d in bootstrap identity envs/dev; do terraform -chdir=$d init -backend=false && terraform -chdir=$d validate; done
terraform -chdir=envs/dev test && terraform -chdir=bootstrap test
terraform -chdir=modules/api init -backend=false && terraform -chdir=modules/api test
tflint --init --config "$PWD/.tflint.hcl" && tflint --recursive --config "$PWD/.tflint.hcl"
```

Lock files are committed with hashes for `linux_amd64`, `windows_amd64` and `darwin_arm64`. After
changing a provider version, run
`terraform providers lock -platform=linux_amd64 -platform=windows_amd64 -platform=darwin_arm64` in each root.

## Cost (dev, low traffic)

Rough figures; check the Azure pricing pages before relying on them.

| Service | Expected |
|---|---|
| Container Apps (Consumption, scale to zero) | ~$0: the monthly free grant covers a pilot |
| PostgreSQL Flexible B1ms + 32 GB | Free for 12 months on a free account (750 h/month), then roughly $15–25/month |
| Static Web Apps Free, GHCR (public) | $0 |
| Log Analytics / Application Insights | Small; the 0.5 GB/day cap bounds it |
| Storage, Key Vault | Cents |
| Entra External ID | Free up to the monthly active user allowance |

The Postgres server is the only line that costs real money after the free year. Stop it when idle with
`az postgres flexible-server stop` (Azure restarts a stopped server after 7 days).

## Later

- **Private networking.** A VNet-integrated Container Apps environment, Postgres with private access
  (a delegated subnet) or a private endpoint, and private endpoints for Storage and Key Vault. Then
  remove the "Allow Azure services" firewall rule. That rule is the main thing to tighten: it admits any Azure
  tenant's traffic to the endpoint, although Entra auth is still required to log in.
- **Entra-only App Insights ingestion.** Set `local_authentication_enabled = false` and grant the API
  identity Monitoring Metrics Publisher (already on the deploy identity's allow-list).
- **Identity state remotely.** `infra/identity` keeps local state because it authenticates to a different
  tenant from the state storage. It holds no secrets, and the registrations can be re-imported.
- **A prod environment.** Copy `envs/dev` to `envs/prod` with its own tfvars and state key, add `prod` to
  bootstrap's `environments`, and gate the GitHub `prod` environment with required reviewers.
