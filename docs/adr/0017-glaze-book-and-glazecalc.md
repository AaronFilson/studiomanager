# 17. Glaze book: member-private recipes, untrusted imports, and Glazecalc integration

- **Status:** accepted
- **Date:** 2026-11-20

## Context

A pottery studio maintains shared bucket glazes (staff-edited, with a target specific gravity,
test-tile photos, and a mix log), while members keep their own recipes privately — often in
[Glazecalc](https://github.com/AaronFilson/glazecalc) (`glazecalcapp.com`) or
[glaze-vault](https://github.com/AaronFilson/glaze-vault).

This introduces five design pressures:
1. **Privacy inside a shared tenant:** members must be able to store private recipes in the same
   studio without other members or staff seeing or editing them, while still allowing opt-in
   sharing and staff promotion to a studio glaze.
2. **Untrusted recipe imports:** export files and share links come from external tools and can be
   oversized, malformed, contain HTML/scripts in notes, or be re-imported multiple times.
3. **Bucket consistency:** studio glazes need a mix log with specific gravity readings, off-target
   warnings, outbound events (`glaze.mixed` webhook and Kafka stream), and assistant tooling
   (`log_glaze_mix` over MCP).
4. **Glaze chemistry without a Java port:** potters want UMF (Seger unity molecular formula),
   Si:Al ratio, Stull surface predictions, theoretical thermal expansion (CTE), cone limit checks,
   and a slurry SG adjustment helper in both web and mobile apps.
5. **Live Glazecalc sync:** members who already use Glazecalc want both one-click share-link
   imports and a live OAuth connection without exposing OAuth tokens at rest or opening SSRF holes.

## Decision

- **Owner-layered RLS on `glaze_recipe`, `glaze_line`, `glaze_photo`, and `glazecalc_connection`:**
  - `glaze_recipe` splits RLS by command (V26): `SELECT` allows `owner_kind = 'STUDIO'`,
    `shared_with_studio`, or `owner_account_id = app.current_account_id()`. `INSERT`/`UPDATE`/
    `DELETE` allow only `STUDIO` (enforced as `glazes.studio.edit` in the app) or the owner.
  - `glaze_line` and `glaze_photo` (V47) follow their parent recipe through `EXISTS` policies so
    neither lines nor test-tile photos can leak across members.
  - `glazecalc_connection` (V48) restricts all commands to `studio_id = current_studio_id() AND
    account_id = current_account_id()`.
- **Studio glaze mix log and `glaze.mixed` events (V46):**
  - `glaze_mix` and `glaze_mix_reading` are restricted to studio glazes (`owner_kind = 'STUDIO'`),
    edited by staff (`glazes.studio.edit`).
  - Readings outside `±0.03` of `target_sg` return an explicit `offTarget` flag and actionable
    warning.
  - Logging a mix raises a `GlazeMixed` domain event inside the transaction, consumed by
    `WebhookFanOut` (`glaze.mixed`) and `StreamPublisher` (`StudioStreamEvent.GlazeMixed`).
  - MCP exposes `log_glaze_mix`, gated on both `glazes.studio.edit` and the `write:glazes` token
    scope (`McpToolCalls` enforces the scope even when a read-only token holds read-side
    permissions).
- **Untrusted JSON import with preview and atomic commit:**
  - `GlazeImportSizeFilter` rejects bodies over 2 MB with RFC 9457 `413` before Jackson parses
    them.
  - `GlazeImportParser` strictly detects `glazecalc-export` v1 and `glaze-vault-export` v1,
    enforces 500 recipes max and 60 lines per recipe max, strips HTML tags/scripts from notes,
    keeps original `amount_text` and per-line oxide `chemistry` JSON snapshots, and maps unknown
    cone/atmosphere text into notes with warnings.
  - `POST /glazes/import?dryRun=true` writes nothing and returns per-recipe outcomes (`NEW`,
    `UPDATED`, `UNCHANGED`, `ERROR`, plus warnings). `dryRun=false` commits atomically only when
    there are zero `ERROR` rows, deduplicating by `(studio_id, owner_account_id, source,
    source_ref)` (using `_id` for Glazecalc and recipe name for glaze-vault).
  - `GET /glazes/{id}/export` emits a valid `glazecalc-export` v1 document for round-trip use in
    Glazecalc.
- **Test-tile photos and promote (V47):**
  - `glaze_photo` reuses the ADR 12 direct-to-blob SAS upload flow under prefix
    `glazes/{studio}/{recipe}/{photo}/full.jpg` and `thumb.jpg` (up to 6 photos per recipe).
  - `GlazePhotoUploadSweep` acts as `SystemActor.PHOTO_CLEANUP` (ADR 16) via
    `app.take_abandoned_glaze_photo_uploads` (`SECURITY DEFINER`, checking
    `app.require_system_principal()`).
  - `POST /glazes/{id}/promote` (`glazes.studio.edit`) copies a shared member recipe into a new
    studio glaze (`source = MANUAL`, `promoted_from = originalId`), leaving the member's copy
    untouched.
- **Shared pure-TypeScript chemistry package (`@studiomanager/glaze-chemistry`):**
  - Modeled on Aaron Filson's MIT-licensed Glazecalc (`lib/chemistry` & `lib/recipe`, with MIT
    attribution), zero runtime dependencies so both `web/` and `mobile/` can import it directly.
  - Uses each line's imported `chemistry` snapshot first, falling back to a built-in library of 60+
    essential ceramic materials and aliases, and reports unmatched materials cleanly.
  - Computes Seger UMF (`R2O + RO = 1.0`, `R2O3`, `RO2`, `R2O:RO`), Si:Al ratio and Stull surface
    prediction, Appen thermal expansion (CTE), Cone 04 / Cone 6 / Cone 10 limit checks, and
    Brongniart/conservation slurry specific gravity adjustments.
- **Live Glazecalc share-link & PKCE OAuth integration (V48):**
  - Share-link imports (`POST /glazes/import-share`) strictly allowlist the configured Glazecalc
    host (`glazecalcapp.com` by default), check resolved IPs against `PublicAddresses` at DNS
    resolution time to prevent SSRF/DNS rebinding, fetch `GET /api/shared/recipe` with
    `X-Share-Secret`, and route the result through the same importer.
  - OAuth uses PKCE (`S256`) with stateless AES-256-GCM encrypted `state` bound to
    `caller.accountId()`. Access and refresh tokens are encrypted at rest via `SecretCipher` bound
    to `connection.id()`. Sync pulls `GET /api/export` with the bearer token (auto-refreshing if
    expired) and runs it through `GlazeImportService`.

## Consequences

- Members bring recipes from Glazecalc or glaze-vault without retyping, and keep exact imported
  oxide chemistry for accurate UMF/CTE/limit analysis on both web and mobile.
- Private member recipes, photos, and OAuth tokens are fenced by PostgreSQL RLS in addition to
  application authorization checks.
- Chemistry math runs client-side with zero server CPU or Java duplication, while batch scaling,
  mix logging, and import validation remain authoritative on the API.
- If Glazecalc OAuth credentials aren't configured in a deployment, file import, export, and
  share-link import continue to work independently.
