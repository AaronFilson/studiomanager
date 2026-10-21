# First deploy: from an empty subscription to "hello studio"

A one-time checklist for each subscription and environment. It assumes the `dev` environment. Allow about
an hour, most of it waiting for Postgres. For background on the design, see
[infra/README.md](../../infra/README.md).

## You need

- An Azure subscription where you are **Owner**, and permission to create Entra groups in its tenant.
- Azure CLI 2.70+ and Terraform 1.16.x locally. (`psql` isn't needed: step 12 uses Azure Cloud Shell.)
- Admin rights on the GitHub repo `AaronFilson/studiomanager`.

## Steps

### Identity provider

1. **Create the Microsoft Entra External ID tenant** (portal only). In the
   [Microsoft Entra admin center](https://entra.microsoft.com): *Entra ID > Overview > Manage tenants >
   Create > External*. Choose a name and a domain prefix (for example `studiomanagerdev`), and link
   the tenant to your subscription for billing. Write down the **tenant ID** and the **domain prefix**.

### Azure foundations

2. **Apply bootstrap** (as yourself, in the subscription's normal tenant):

   ```bash
   az login
   cd infra/bootstrap
   cp terraform.tfvars.example terraform.tfvars   # set subscription_id
   terraform init
   terraform apply
   terraform output        # keep this: github_variables and db_admin_groups
   ```

   This creates the state storage, the GitHub deploy and plan identities, `rg-studiomanager-dev`,
   and the Entra group `studiomanager-dev-db-admins` with you in it, and registers the resource
   providers. State stays local in `infra/bootstrap/terraform.tfstate` (git-ignored). Back it up.

3. **Apply identity** (signed in to the **External ID** tenant):

   ```bash
   az login --tenant <external-tenant-id> --allow-no-subscriptions
   cd infra/identity
   cp terraform.tfvars.example terraform.tfvars   # tenant ID and domain prefix from step 1
   terraform init
   terraform apply
   terraform output env_tfvars
   ```

   This registers `studiomanager-dev-api` (scope `access_as_user`, v2 tokens) and `studiomanager-dev-web`
   (SPA, redirect URI `http://localhost:5173/`), and grants admin consent.

4. **Create the sign-up/sign-in user flow** (portal, External ID tenant): *External Identities > User
   flows > New user flow*. Pick *Email with password* (or *Email one-time passcode*), and collect
   *Display Name*. Then open the flow, choose *Applications > Add application*, and add
   **studiomanager-dev-web**. Without this step, sign-in reports that the app has no user flow.

5. **Fill in `infra/envs/dev/dev.tfvars`**: the four `entra_*` values from step 3, and
   `db_admin_group.object_id` from `terraform output db_admin_groups` in step 2. Commit this on a branch.
   The plan refuses to run while any `REPLACE_ME` is left.

### GitHub

6. **Create the `dev` environment**: *Settings > Environments > New environment > `dev`*. Under
   *Deployment branches and tags*, choose *Selected branches and tags* and add `main`. This is what
   makes the deploy identity unusable from other branches (it trusts only `environment:dev`).
   Optionally add yourself as a required reviewer.

7. **Set the repository variables** (*Settings > Secrets and variables > Actions > Variables*) to the
   values in bootstrap's `github_variables` output:

   | Variable | Value |
   |---|---|
   | `AZURE_TENANT_ID` | workforce tenant ID |
   | `AZURE_SUBSCRIPTION_ID` | subscription ID |
   | `AZURE_CLIENT_ID_DEPLOY` | client ID of `id-studiomanager-github-deploy` |
   | `AZURE_CLIENT_ID_PLAN` | client ID of `id-studiomanager-github-plan` |
   | `TFSTATE_STORAGE_ACCOUNT` | `sttfstudiomgr<hash>` |

   None of these is a secret, and no GitHub secrets are needed. Until `AZURE_CLIENT_ID_DEPLOY` is set,
   the deploy workflow skips itself, and PR plans skip until `AZURE_CLIENT_ID_PLAN` is set.

   With the GitHub CLI: `gh variable set AZURE_TENANT_ID --body <value>` (and so on).

### First deploy

8. **Merge the `dev.tfvars` PR.** Its CI run includes a *Terraform plan (dev)* summary (read-only
   identity). The merge to `main` starts **Deploy dev**.

9. **Make the image public (once).** The first run's *Build and push API image* job creates the
   `studiomanager-api` package on GHCR. New packages start private, so *Terraform apply* stops at
   "Image is publicly pullable". Go to your profile's *Packages > studiomanager-api > Package settings >
   Change visibility > Public*. Then *Re-run failed jobs*.

10. **Let Terraform apply.** This takes 10–20 minutes; Postgres is the slow part. The job summary lists
    the API URL, the web URL, and the identity names.

11. **Expect the first release to fail its smoke test.** *Migrate database and roll out API* runs
    Flyway as the migrator, which creates the schema and the `app_rw` role. The API identity has no
    Postgres login yet, so the readiness probe (which includes the database) fails. Step 12 fixes that.

12. **Create the API identity's Postgres principal (once per environment).** Terraform can't do this.
    Open **Azure Cloud Shell** (Bash) in the portal. Its traffic is covered by the server's
    "Allow Azure services" firewall rule, and it has `az` and `psql`. Then run:

    ```bash
    export PGHOST=$(az postgres flexible-server list -g rg-studiomanager-dev --query "[0].fullyQualifiedDomainName" -o tsv)
    export PGUSER=studiomanager-dev-db-admins      # Entra group admin: sign in with the group's name
    export PGDATABASE=postgres                     # pgaadauth_create_principal must run in the postgres database
    export PGSSLMODE=require
    export PGPASSWORD=$(az account get-access-token --resource-type oss-rdbms --query accessToken -o tsv)

    psql -c "select * from pgaadauth_create_principal('id-studiomanager-dev-api', false, false);"
    ```

    Expect `Created role for "id-studiomanager-dev-api"`. The role name must match the managed
    identity's name exactly, and `false, false` means not an admin and no MFA check. Reference: Microsoft
    Learn, [Connect with managed identity](https://learn.microsoft.com/azure/postgresql/security/security-connect-with-managed-identity)
    and [Manage Microsoft Entra roles](https://learn.microsoft.com/azure/postgresql/security/security-manage-entra-users).

    Do not grant anything by hand. The grant belongs to the migrations, as described in step 13.

13. **Re-run failed jobs** on the Deploy dev run. The migration job runs again, and its Azure-only
    callback (`api/src/main/resources/db/callback/azure/afterMigrate__grant_app_rw_to_api.sql`) now
    finds the role and grants it `app_rw`. Then the API rolls out, the smoke test passes, and the web
    app deploys to Static Web Apps.

14. **Register the web origin for sign-in.** Add the web URL from the job summary to
    `infra/identity/terraform.tfvars`:

    ```hcl
    web_origins = ["http://localhost:5173", "https://<swa-default-host-name>"]
    ```

    Then run `terraform apply` again in `infra/identity`, signed in to the External ID tenant. Entra
    registers `https://<swa-default-host-name>/`, with the trailing slash, which is the URI the web app
    redirects to.

15. **Check "hello studio".** Open the web URL, sign up, and create a studio. The API's readiness
    endpoint is `<api-url>/actuator/health/readiness`.

16. **Tidy up.**
    - Protect `main` and require the CI checks (*API*, *Web and API client*, *Terraform*).
    - While a pilot studio uses dev, set `api_min_replicas = 1` in `dev.tfvars` to avoid cold starts.

## Later changes to keep in mind

- **Custom API domain:** `web/public/staticwebapp.config.json` sets a Content-Security-Policy
  whose `connect-src` allows only `https://*.azurecontainerapps.io` and `https://*.ciamlogin.com`. Add any
  new API host there. M1's direct-to-Blob photo uploads will need the storage endpoint
  (`https://<account>.blob.core.windows.net`) there too. Also update `STUDIOMANAGER_CORS_ALLOWED_ORIGINS`
  (Terraform sets it to the Static Web App origin) when the web app gets a custom domain.
- **A new environment:** add it to bootstrap's `environments`, then repeat steps 2–16 with its name.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `AADSTS50011` redirect URI mismatch | Step 14 not done, or an origin registered with a path. Entra matches `<origin>/` exactly. |
| `AADSTS700016` / app not found during sign-in | The web app isn't added to the user flow (step 4), or the wrong tenant subdomain in `dev.tfvars`. |
| API returns 401 for a fresh token | Issuer or audience mismatch: the token's `iss` must be `https://<tenant-id>.ciamlogin.com/<tenant-id>/v2.0` and `aud` the API client ID. Check `STUDIOMANAGER_AUTH_*` on the container app. |
| Browser CORS error calling the API | `STUDIOMANAGER_CORS_ALLOWED_ORIGINS` doesn't include the page's origin. |
| `No function matches ... pgaadauth_create_principal` | Connected to `studiomanager` rather than `postgres`. |
| `Could not validate AAD user ... name is not found` | Role name differs from the managed identity's name. |
| psql `password authentication failed` as the group | The token predates your group membership. Wait a few minutes, then fetch a new token. |
| Migration job failed | Portal: *caj-studiomanager-dev-migrate > Execution history > Console logs*, or `ContainerAppConsoleLogs_CL` in Log Analytics. |
| `ServerBusy` from Postgres during apply | Another server operation was in flight. Re-run the job. |
| Plan fails with `Must be a GUID` | A `REPLACE_ME` is still in `dev.tfvars` (step 5). |
