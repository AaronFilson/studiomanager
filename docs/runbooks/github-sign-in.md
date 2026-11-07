# Turning on GitHub sign-in

The GitHub sign-in bridge (ADR 20, `auth/`) is built and deployed by the normal pipeline, but
Terraform creates nothing for it until `github_sign_in.enabled` is true. Do this after the first
deploy (docs/runbooks/first-deploy.md), in this order. `<env>` is `dev`.

## 1. Find the names

From `infra/envs/<env>`:

- The vault: `terraform state show module.key_vault.azurerm_key_vault.this` (its `name`).
- The bridge's future host: `ca-studiomanager-<env>-auth.<default domain>`. The environment's
  default domain is in the portal (Container Apps environment > Overview), or
  `az containerapp env show -n cae-studiomanager-<env> -g rg-studiomanager-<env> --query properties.defaultDomain -o tsv`.
  So the issuer is `https://ca-studiomanager-<env>-auth.<default domain>`.

## 2. Create the GitHub OAuth app

GitHub > Settings > Developer settings > OAuth Apps > New OAuth App (an OAuth app, not a GitHub
App):

- **Application name:** Studio Manager (`<env>`)
- **Homepage URL:** the web app's URL.
- **Authorization callback URL:** `<issuer>/login/oauth2/code/github`, exactly.
- Leave device flow off.

Copy the **client ID** (not a secret) and generate a **client secret**.

## 3. Put the three secrets in Key Vault

You need Key Vault Secrets Officer on the vault (the vault is RBAC-only). Use a shell that writes
nothing to history, and delete the key file afterwards.

```sh
vault=<vault name>

# The bridge's signing key: RSA 2048, PKCS#8 PEM
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out bridge-signing-key.pem
az keyvault secret set --vault-name "$vault" --name github-bridge-signing-keys --file bridge-signing-key.pem --output none
rm bridge-signing-key.pem

# The GitHub OAuth app's client secret
az keyvault secret set --vault-name "$vault" --name github-bridge-github-client-secret --value '<GitHub client secret>' --output none

# The secret Entra presents to the bridge. Keep it for step 5, then forget it.
entra_secret=$(openssl rand -base64 48)
az keyvault secret set --vault-name "$vault" --name github-bridge-entra-client-secret --value "$entra_secret" --output none
```

## 4. Turn the bridge on

1. Make the bridge's image public: GitHub > Packages > `studiomanager-auth` > Package settings
   > Change visibility. (Every deploy pushes it, so it exists already.)
2. In `infra/envs/<env>/<env>.tfvars`:

   ```hcl
   github_sign_in = {
     enabled          = true
     github_client_id = "<GitHub client ID>"
   }
   ```

   `entra_client_id` defaults to `studiomanager-entra`; it's an identifier, not a secret.
3. Merge to `main`. The deploy creates `ca-studiomanager-<env>-auth`, its identity and its Key
   Vault role, and later deploys roll its image. The job summary's Terraform outputs include
   `bridge_issuer`.
4. Check it: `curl https://<bridge host>/.well-known/openid-configuration` shows the issuer, and
   `/oauth2/jwks` one key.

If the Container App fails to create because a secret can't be read, the role assignment may
not have propagated yet: re-run the failed job.

## 5. Add the bridge to Entra External ID

The `azuread` Terraform provider can't manage custom OIDC identity providers (a Microsoft Graph
beta resource), and the request carries a secret that `infra/identity`'s local state shouldn't
hold, so this is a manual step. Sign in to the **external** tenant as an External Identity
Provider Administrator: `az login --tenant <external-tenant-id> --allow-no-subscriptions`.

Write the body to a file (not the command line), then post it:

```json
{
  "@odata.type": "#microsoft.graph.oidcIdentityProvider",
  "displayName": "GitHub",
  "clientId": "studiomanager-entra",
  "issuer": "<issuer>",
  "wellKnownEndpoint": "<issuer>/.well-known/openid-configuration",
  "responseType": "code",
  "scope": "openid profile email",
  "clientAuthentication": {
    "@odata.type": "#microsoft.graph.oidcClientSecretAuthentication",
    "clientSecret": "<entra_secret from step 3>",
    "clientAuthenticationMethod": "client_secret_post"
  },
  "inboundClaimMapping": {
    "sub": "sub",
    "name": "name",
    "email": "email",
    "email_verified": "email_verified"
  }
}
```

```sh
az rest --method post --url https://graph.microsoft.com/beta/identity/identityProviders --body @github-idp.json
rm github-idp.json
```

Or in the admin center: Entra ID > External Identities > All identity providers > Custom > Add
new > Open ID Connect, with the same values (client authentication `client_secret_post`, response
type `code`, and the claims mapping above).

Then add it to the sign-up and sign-in user flow: External Identities > User flows > the flow >
Identity providers > tick **GitHub** > Save.

## 6. Test

1. Open the web app in a private window and sign in. The Entra page shows **GitHub**.
2. It goes to the bridge, then GitHub; approve the app. You land back in Studio Manager.
3. In the external tenant, the new user's identities show the bridge's issuer, and the subject
   `github:<your numeric GitHub id>`.
4. A GitHub account whose primary email isn't verified gets the bridge's "needs a verified primary
   email" page, and no account.
5. Send an invite to your GitHub account's verified email, and accept it by signing in with
   GitHub.
6. If anything fails, the bridge's logs are in Log Analytics (`ContainerAppConsoleLogs_CL`,
   filtered to `ca-studiomanager-<env>-auth`).

Then update ADR 20's "Unverified" list with what the real sign-in showed: whether Entra sent a
PKCE challenge (if it did, set `requireProofKey(true)` in `BridgeSecurity`), and whether the
user was linked to an existing email account.

## Rotating secrets

- **Signing key:** generate a new key and set the secret to the new PEM followed by the old one
  (`cat new.pem old.pem`). Restart the bridge's revision. A day later, set it to the new key
  alone and restart again.
- **GitHub client secret:** generate a second one in the OAuth app, set the vault secret, restart
  the revision, then delete the old one on GitHub.
- **Entra's client secret:** set a new vault secret, restart, and `PATCH` the identity provider's
  `clientAuthentication` with it straight away; sign-ins in between fail.

## Turning it off

Remove the user flow's GitHub tick first, then set `enabled = false`. The vault secrets stay
until you delete them.
