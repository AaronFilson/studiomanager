# 6. Terraform, and CI/CD with no stored secrets

- **Status:** accepted
- **Date:** 2026-10-19

## Context

Infrastructure must be reproducible and reviewable, and it should use tools that transfer
between employers and clouds. CI/CD that deploys to Azure usually holds a client secret in GitHub,
which then has to be rotated and can leak.

## Decision

- **Terraform** (azurerm 5.x, azuread 3.x), rather than Bicep, because it's the
  industry-transferable choice. There are three root modules, applied in order:
  1. `infra/bootstrap`: state storage, the CI identities, and the environment's resource group.
     Applied once by a person, with local state.
  2. `infra/identity`: the External ID app registrations. Applied by a person signed in to the
     External ID tenant.
  3. `infra/envs/dev`: everything else, with remote state in Azure Storage using Entra
     authentication, not shared keys.
- **GitHub OIDC federation:** workflows sign in to Azure as user-assigned managed identities
  that trust specific GitHub subjects. No client secrets are stored anywhere.
  - The **plan** identity (pull requests) can only read.
  - The **deploy** identity trusts only the `dev` GitHub environment. Its rights are confined to
    the environment's resource group, and it may grant only three named data roles.
- **Plan on PR, apply on merge.** CI also runs `terraform test` with mocked providers, tflint and
  actionlint.
- **Images go to GHCR.** The repository is public, so the images are too, and Container Apps pulls
  them anonymously. This avoids an ACR bill and a registry credential.

## Consequences

- Rebuilding an environment is a handful of commands ([runbook](../runbooks/first-deploy.md)).
- `prevent_destroy` guards stateful resources (Postgres, storage, Key Vault, state).
- Public images mean nothing secret may ever be baked into an image. That's good practice anyway.

## In an interview

"No secrets in CI. GitHub's OIDC token is federated to a managed identity scoped to one resource
group, and pull requests get a separate read-only identity. Terraform rather than Bicep because
it's what most teams use, and the state backend uses Entra auth, not storage keys."
