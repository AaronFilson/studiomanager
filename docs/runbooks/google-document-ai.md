# Google Document AI and Azure Document Intelligence for invoices

The platform-paid invoice readers (docs/planning/20, "Invoice readers"; ADR 18). Azure's comes up
with `infra/envs/dev` on the next deploy. Google's needs a Google Cloud project and one apply of
`infra/gcp` by hand. Neither reads anything until the admin page turns it on for a studio and the
studio's owner consents.

## Azure AI Document Intelligence

Nothing to do by hand except one bootstrap re-apply, because bootstrap holds two things it needs:

1. **Re-apply `infra/bootstrap`** as subscription Owner. It now registers
   `Microsoft.CognitiveServices` and lets the deploy identity grant *Cognitive Services User* to
   the API identity.
2. **Deploy** as usual. `envs/dev` creates `di-studiomanager-dev-<hash>` on F0 (free, 500 pages a
   month, first two pages of each file, 4 MB per file) with key auth off, grants the API identity
   *Cognitive Services User*, and gives the API `STUDIOMANAGER_INVOICES_AZURE_ENDPOINT`.

A subscription can have only one F0 Document Intelligence account. If another exists, delete it,
or set `sku_name = "S0"` on the module (paid by the page) and set
`STUDIOMANAGER_INVOICES_AZURE_USD_PER_THOUSAND_PAGES` so the spend estimate is right.

## Google Document AI

### 1. A Google account

Any Google account works. A new Google Cloud account gets a free trial credit, which covers far
more than the demo: the Invoice Parser costs $0.10 per 10 pages, billed per document in blocks of
10 pages ([pricing](https://cloud.google.com/document-ai/pricing)).

1. Go to <https://console.cloud.google.com> and sign in.
2. Accept the terms. Start the free trial if offered: it asks for a card, but doesn't charge it
   unless you upgrade the billing account.

### 2. A project, with billing linked

In the console, or with the gcloud CLI (<https://cloud.google.com/sdk/docs/install>):

```bash
gcloud auth login
# Pick a globally unique id: 6-30 lowercase letters, digits and hyphens
gcloud projects create studiomanager-dev-<something-unique> --name="Studio Manager dev"
gcloud billing accounts list                      # note the ACCOUNT_ID
gcloud billing projects link studiomanager-dev-<something-unique> --billing-account=<ACCOUNT_ID>
```

In the console: **IAM & Admin → Create a project**, then **Billing → Link a billing account**.

Optional, recommended: **Billing → Budgets & alerts**, a budget of a few dollars for the project,
with email alerts at 50% and 100%.

### 3. The values for `infra/gcp`

| Variable | Where it comes from |
|---|---|
| `project_id` | The project id from step 2 (not its name or number) |
| `document_ai_location` | `us` or `eu`: where invoices are processed |
| `azure_tenant_id` | `az account show --query tenantId -o tsv`: the subscription's tenant, **not** the External ID tenant |
| `api_principal_id` | `terraform output api_identity_principal_id` in `infra/envs/dev` (after a deploy) |

Copy `infra/gcp/terraform.tfvars.example` to `infra/gcp/terraform.tfvars` (git-ignored) and fill
them in.

### 4. Apply `infra/gcp`

```bash
gcloud auth application-default login
cd infra/gcp
terraform init
terraform providers lock -platform=linux_amd64 -platform=windows_amd64 -platform=darwin_arm64
terraform apply
```

It enables Document AI, IAM, IAM Credentials and STS; creates the Invoice Parser processor; a
workload identity pool whose provider trusts Entra tokens for the API identity only; and a
service account with `roles/documentai.apiUser` that only that identity may impersonate. No
service-account key exists anywhere. State is local and git-ignored, like `bootstrap` and
`identity`; it holds no secrets.

Commit `infra/gcp/.terraform.lock.hcl`, and add `gcp` to the root-module loop in
`.github/workflows/ci.yml` (`for dir in bootstrap identity envs/dev gcp`) so CI validates and
tests it offline. CI never applies it.

### 5. Give the API the values

```bash
terraform output api_env
```

Paste it into `infra/envs/dev/dev.tfvars` as `google_invoice_reader = { ... }`, commit, and let CI
deploy. The API then has `STUDIOMANAGER_INVOICES_GOOGLE_*`.

### 6. Check the token's issuer on first use

The pool provider trusts the issuer `https://login.microsoftonline.com/<tenant>/v2.0` and the
audience `api://AzureADTokenExchange` by default. That's what a managed-identity token for that
audience is expected to carry, but it hasn't been checked against a real one. If the first read
fails with "Google Document AI refused the platform's sign-in", look at a real token's claims:

```bash
az containerapp exec -n ca-studiomanager-dev-api -g rg-studiomanager-dev --command sh
# inside the container
curl -s -H "X-IDENTITY-HEADER: $IDENTITY_HEADER" \
  "$IDENTITY_ENDPOINT?api-version=2019-08-01&resource=api://AzureADTokenExchange&client_id=$AZURE_CLIENT_ID" \
  | cut -d'"' -f4 | cut -d. -f2 | base64 -d 2>/dev/null
```

Set `azure_token_issuer` (and `azure_token_audience` if `aud` differs) in `terraform.tfvars` to
match `iss`, and apply again. `sub` must equal `api_principal_id`.

## Turning them on

1. The studio's owner gives consent (`allowPlatformReaders`) when saving the studio's AI key in
   Settings. Without it, no platform reader reads that studio's invoices. The consent is stored
   with the key, so a studio with no Anthropic key can't give it yet.
2. On **Admin → Invoice readers**: turn a reader on, choose the studios (or every studio that
   consents), and set the monthly page caps per studio and for the platform. The page shows this
   month's pages and estimated spend.
3. Optionally give a studio its own order, such as "Google only" for the demo.
4. On an invoice, **Compare readers** reads it with every reader the studio may use and shows the
   fields side by side with time and cost. Those runs count toward the caps.

Before any rules are saved, both readers are on for the demo studio only
(`STUDIOMANAGER_INVOICES_DEMO_STUDIO_ID`, from `invoice_demo_studio_id` in `dev.tfvars`), and off
for everyone else.

## The eval

With credentials, the live eval reads every labelled invoice with each reader and writes a report
into `docs/evals`:

```bash
STUDIOMANAGER_EVAL_GOOGLE_TOKEN=$(gcloud auth print-access-token) \
STUDIOMANAGER_EVAL_GOOGLE_LOCATION=us \
STUDIOMANAGER_EVAL_GOOGLE_PROCESSOR=<processor_name from api_env> \
  bash scripts/api.sh test --tests '*LiveDocumentServiceEval'
```

For Azure, `az login` as someone with *Cognitive Services User* on the account, then set
`STUDIOMANAGER_EVAL_AZURE_ENDPOINT`. Your own Google account needs `roles/documentai.apiUser` on
the project (an Owner has it).
