# Branch protection for `main`

A setting on the GitHub repo, so the owner applies it once: **Settings → Rules → Rulesets → New
branch ruleset**. A ruleset rather than classic branch protection, because it can be exported as
JSON and reviewed.

| Setting | Value | Why |
|---|---|---|
| Target | the default branch | |
| Restrict deletions | on | |
| Block force pushes | on | History on `main` is what was reviewed and deployed. |
| Require a pull request before merging | on, 0 approvals | One maintainer: the PR is for the checks and the record, not for a second reviewer. |
| Require status checks to pass | on, branches up to date | |
| Required checks | `API (build and test)`, `Web, mobile and API client`, `Terraform (fmt, validate, test, lint)`, `CodeQL (java-kotlin)`, `CodeQL (javascript-typescript)`, `CodeQL (actions)` | The job names in `.github/workflows/ci.yml` and `codeql.yml`. A check appears in the picker only after it has run once. |
| Require code scanning results | CodeQL, alerts of High or higher | New high-severity findings block the merge; older ones don't. |
| Bypass list | Repository admin, "for pull requests only" | So the owner can still merge a PR in an emergency, but never push straight to `main`. |

`Terraform plan (dev)` is not required: it is skipped until Azure is set up, and skipped checks
would block every PR.

**Also turn on** (Settings → Code security): Dependabot alerts, Dependabot security updates, and
secret scanning with push protection. Dependabot version updates are configured in
`.github/dependabot.yml`.
