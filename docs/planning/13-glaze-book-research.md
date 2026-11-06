# 13. Glaze book: research for importing recipes from Glazecalc

This is research for item 5 in `08-wave2-queue.md`. It was done on 2026-11-05 by reading the public repo
https://github.com/AaronFilson/glazecalc (the default branch, through raw.githubusercontent.com and the GitHub
contents API) and the live site https://glazecalcapp.com. Short paths below are in that repo.
**Unverified** marks anything I inferred and did not read in the code.

## What Glazecalc is

- The app is Angular 22, not React, with strict TypeScript (`client/`). The server is Express 5 with
  Mongoose 9 on MongoDB (`server/`). Chemistry shared by the browser, the server and the MCP tools is in
  `lib/chemistry/` and `lib/recipe/`. Source: `README.md`.
- It has real accounts: Google, GitHub or Microsoft sign-in, or a password, with passkeys or TOTP as a
  second step. A JWT is kept in an httpOnly cookie. Trial accounts are deleted after 14 days. Data is
  stored on the server in MongoDB, not only in the browser. Source: `README.md`.
- The live site was up. It says the calculator works without an account and that an account keeps
  recipes and notes.

## 1. Recipe data model (`server/models/recipe.ts`)

- `title` (required), `date` (string), `notes` (a list of strings), `ownedBy`, `updatedAt`.
- `materials` (required) and `additives` hold the ingredient lines. They are `Mixed` in Mongoose. Their
  shape comes from the shared types:
  - A material line is a full copy of the material plus its amount: `MaterialRecord extends MaterialInput`
    with `amount?: string` ("as saved: \"12.5\""), in `lib/recipe/library.d.ts`. `MaterialInput` is
    `{ name, formula?, analysis?: Record<oxide, number|string>, fields?: {name, amount, amountUnity?}[],
    percentmole?, loi?, molecularweight?, equivalent? }`, in `lib/chemistry/index.d.ts`. The line's
    `_id` links it to a standard material or to the user's own material (`server/lib/import_data.ts`).
  - Amounts are strings. The base materials are percent or parts. Each additive has
    `unit?: 'percent' | 'parts' | 'grams'` (`AdditiveUnit` in `lib/recipe/library.d.ts`), so colorants can
    be a percent of the base, parts, or grams. `includeAdditives` sets whether additives count toward the
    unity formula.
  - Unverified: whether base material amounts must sum to 100. The README says scaling to percent is done
    on output.
- Firing: `firedAt` is the firing as the potter states it, cleaned by `firing.cleanFiring`. `firing` is
  a hand-picked band from `limits.FIRING_BANDS`. `coneResults` is a list of `{ cone, atmosphere?, summary }`.
  `firings` holds up to 200 links `{ firing (firing-log id), result (500 characters at most), cone,
  atmosphere }`. The atmosphere values come from `firing.ATMOSPHERES`. I did not read that list (unverified).
- Other fields: `foodSurface` (boolean, for liners), `sgTarget` (specific gravity, 1 to 2.5), and
  `computed` (the unity formula, computed in the browser and cached).
- Versions: `rev` is a revision counter used with If-Match. A `record_version` model and
  `server/lib/record_history.ts` exist, so there is a version history. That history is not exported
  (see section 2).
- Images: the recipe has no image field. Unverified: photos may attach to firing logs or notes.

## 2. Export, import, share and API

- Export (`server/routes/export_routes.ts`, `server/lib/export_data.ts`). Each endpoint needs a signed-in
  or trial session (`jwtAuth`).
  - `GET /api/export` returns the whole account as JSON: `{ format: "glazecalc-export", version: 1,
    exportedAt, account, preferences, shelf, prices, records }`. `records` has one array per kind, oldest
    first. The kinds are materials, additives, firing, advice, recipes and notes (the import list in
    `import_data.ts`). Each record keeps all its fields except `ownedBy` and `__v`.
  - `GET /api/export/recipes.csv` has one row per ingredient, with the columns `Recipe, Date, Notes, Type,
    Ingredient, Amount, Unit of additive, Cone, Atmospheres, Food surface`. Type is `Material` or `Additive`.
    It has no material ids and no chemistry.
  - `GET /api/export/materials.csv` lists the user's own materials.
- Import (`server/lib/import_data.ts`) accepts only `glazecalc-export` files, version 1 up to the current
  version, through an upgrade chain that is empty so far. Lines that point to an own material not in the
  file keep their copied chemistry without the link (warning `line-unlinked`).
- Share links (`server/routes/share_routes.ts`). The owner creates a link with
  `POST /api/share/recipe/:id {includeNotes}`. The public read is `GET /api/shared/recipe`, with the
  secret in the `X-Share-Secret` header. The page URL is `/shared#<secret>`. I did not read the response
  shape (`server/lib/share_links.ts`), so it is unverified.
- API: there is a JSON API under `/api` that uses the cookie session. There is also an MCP endpoint at
  `/mcp`, with Glazecalc's own OAuth server and token scopes (`server/lib/scopes.ts`,
  `require_scope.ts`). Unverified: whether a third-party app such as studiomanager could get an OAuth
  token for plain REST reads, or only for MCP.

## 3. Materials and analysis

- The standard library has 182 materials and 37 colorants and additives, from manufacturer data sheets.
  Each has a source, other names, hazards, and replacements for discontinued materials. It lives in
  `data/`. Source: `README.md`.
- Analysis code is in `lib/chemistry/` (`umf.js`, `oxides.js`, `expansion.js`, `fit.js`) and
  `lib/recipe/` (`limits`, `cone-fit`, `cost`, `slurry`, `hazards`). It covers the unity formula,
  Si:Al ratio, expansion, limit formulas and a Stull chart. All of it is MIT and pure TypeScript/JS.
- Worth carrying over: the name of each line and its amount, and for each line the oxide analysis and
  LOI. Each line already holds them, so an imported recipe stands on its own without our own materials
  database. Also keep `computed`, but only as a cached snapshot.
- Recompute or leave out: limit checks, cone fit, cost and slurry. The studio app is not a glaze
  calculator. If it ever needs a unity formula, it could call `lib/chemistry` from the web client, which
  the MIT license allows, rather than port it to Java.

## 4. License

MIT, "Copyright (c) 2017-2026 Aaron Filson / Candling Development Studio" (`LICENSE`). The fonts are
under OFL, with notices in `THIRD-PARTY-NOTICES.md`.

## 5. glaze-vault

See the full section `## glaze-vault` at the end. In short, it is a separate product with its own export
format (`glaze-vault-export`), and its batch sheets and mix log are the most useful ideas for the studio.

## Recommendation for the import

**Format to accept.** Accept the `glazecalc-export` JSON file, version 1. A member uploads the file they
downloaded from `GET /api/export`. Reject any other `format`, and any `version` above 1. Read only
`records.recipes`, and leave every other key alone: account, preferences, shelf, prices, notes, advice,
the firing logs and own materials. The line copies already carry the chemistry. As an option, accept
`recipes.csv` too, as a fallback that keeps only names and amounts. Also accept `glaze-vault-export`
v1 (see `## glaze-vault`). Don't call the Glazecalc API or use
share links yet. Both depend on Glazecalc's cookie session or an unverified OAuth path. Revisit an OAuth
connection when Glazecalc publishes a REST scope for it.

**Field mapping into a studio glaze book entry.**

| Glazecalc | Studio glaze book |
|---|---|
| `_id` | `source_ref` (with `source = 'glazecalc'`), for re-import and dedup per member |
| `title` | `name` |
| `date` | `source_date` (free text) |
| `notes[]` | `notes` (joined with blank lines) |
| `materials[].name`, `.amount` | ingredient lines: `kind = BASE`, `name`, `amount` (decimal, parsed from the string), `unit = PARTS` |
| `additives[].name`, `.amount`, `.unit` | ingredient lines: `kind = ADDITION`, `unit` = percent, parts or grams |
| `materials[]._id`, `analysis`, `loi` | an optional `chemistry` JSON per line, kept as it was sent and never edited |
| `firedAt` / `coneResults[].cone`, `.atmosphere` | `cone` and `atmosphere` (the studio's enums; anything unknown goes into notes) |
| `foodSurface` | `food_surface` (a flag the studio can show next to its own food-safety rules) |
| `sgTarget` | `specific_gravity` (optional) |
| `rev`, `updatedAt` | `source_rev` and `source_updated_at`, to detect a newer copy on re-import |

**Leave out:** `computed` (recompute it if it is ever needed), `firings` links (they point to firing logs
that are not imported), `includeAdditives`, `firing` band overrides, version history, share links,
prices, costs, the shelf, `ownedBy`/`__v`, and every account or preferences field. Store the amounts as
they were written (as text along with the parsed decimal), so a recipe in parts is not silently
normalized. The import has no images, because Glazecalc recipes have none. Photos stay a studio-side
feature.

**Safety:** treat the file as untrusted input. Set a size limit on the upload and a limit on the number of
recipes and lines, validate every field, and remove HTML from notes. Write nothing to the studio until a
preview step, as Glazecalc's own import does with a per-line readout.

## glaze-vault

Read on 2026-11-05 from https://github.com/AaronFilson/glaze-vault (default branch, through the git tree API
and raw.githubusercontent.com). Short paths below are in that repo. **Unverified** marks inference.

### What it is

- A desktop app for encrypted glaze and clay recipes kept on the user's own computer, with no accounts and
  no cloud. It is aimed at studios and industry with proprietary recipes. Tauri 2 with a Rust core
  (`crates/core`, `crates/sync`, `crates/hostd`) and a React UI (`ui/`). Source: `README.md`,
  `docs/adr/0001-tauri-rust-react.md`.
- Features (`README.md`, `ui/`): recipes with unity formula, own materials, revisions, compare, print/PDF,
  backups, import and export, search, roles with shared and private collections, a firing log with photos,
  line blends, limit formulas, costs, batch sheets (`ui/batch.ts`) and a mix log (`ui/mix.ts`). Planned and
  partly built: a LAN host for other laptops (`crates/hostd`, ADR 22-25) and phone builds (ADR 26-33).
- Maturity: the README says "Early development", with no release yet. There are 35 ADRs, user docs in
  `docs/user/`, Vitest tests beside most `ui/*.ts` files, Rust tests in `crates/sync/tests/`, end-to-end
  scenarios in `e2e/`, and CI in `.github/workflows/ci.yml`. The commits I saw run from 2026-10-30 to
  2026-11-04, so it is about a week old (the full commit count is unverified).

### Data model, storage and encryption

- A recipe (`docs/adr/0013-recipe-details-and-additions.md`, `ui/export.ts`): `name`, `lines` of
  `{material, amount, addition}`, `notes`, and optional details: type (glaze, clay body, slip, engobe,
  casting slip, terra sigillata), `cone` (free text such as "6" or "04-02"), `atmosphere`, `surface`,
  `transparency`, `color`, `tags`, plus `gravity`, `viscosity`, `water` and `waterBasis` (dry or wet), and
  clay-body fields (absorption, shrinkage, forming). The lists follow Glazy's. Every revision keeps the
  full content. ADR 13 says specific gravity is not in the recipe, but the export has a `Gravity` column,
  so it was added later (unverified which ADR).
- Additions: a line marked as an addition is a percent of the base (the unmarked lines). This is the same
  idea as Glazecalc's additives, but without per-line units: everything is in the same parts.
- Storage (`docs/adr/0003-encryption-and-keys.md`): the vault file is SQLCipher 4 (AES-256, HMAC-SHA512 per
  page). Each record is also encrypted with XChaCha20-Poly1305 under a random 256-bit collection key.
  Collection keys are shared with users through HPKE (X25519). A user's private key is wrapped with an
  Argon2id key from their password (64 MiB, 3 passes). Each user and the organization get a recovery key
  shown once as BIP 39 words. Key epochs replace re-encryption on revoke (ADR 17).
- Plain export (`ui/export.ts`, not encrypted, and the app warns about that):
  - JSON `{ format: "glaze-vault-export", version: 1, exportedAt, appVersion, recipes, materials }`. Each
    recipe is `name`, the detail fields, `lines`, `notes`, `revision`, `savedAt`. Each material is `name`,
    `category`, `basis`, `oxides`, `loi`, `formula`, `class`, `sheet`, `notes`.
  - A recipe CSV with 25 columns (`Recipe, Material, Amount, Percent, Added, Type, Cone, Atmosphere, ...,
    Gravity, Viscosity, ..., Water, Water basis, Notes, Revision, Saved`), plus materials, firings and
    mixes CSVs, and Insight XML for Digitalfire Insight-live and Glazy.
- Import (`ui/importFormats.ts`, `docs/adr/0010-importing.md`): its own JSON and CSVs, Insight XML (also
  Glazy), GlazeChem `.glz`/`.mat`, and a generic materials CSV. It does **not** read `glazecalc-export`,
  and Glazecalc does not read `glaze-vault-export`. It sets no file size limit.
- Mapping to `glazecalc-export` v1: `name` = `title`; `lines[].material`/`amount` = `materials[].name`/
  `amount`; a line with `addition` = an entry in `additives` with `unit: 'percent'`; `cone`/`atmosphere`
  are free text where Glazecalc has `firedAt` and `coneResults`; `gravity` = `sgTarget`; `notes` is one
  string, not a list. Lines carry only the material name, not its chemistry, so the analysis is in the
  separate `materials` array, matched by name. There is no `foodSurface`, and no stable record id in the
  export (only `revision`), so re-import dedup would be by name (unverified: ids may exist in the vault).

### Batch maker (`ui/batch.ts`) and mix log (`ui/mix.ts`)

- `scaleBatch(lines, size)`: base = the sum of non-addition amounts (all lines if every line is an
  addition). For each line, `percent = amount / base * 100` and `weight = amount / base * size`. Additions
  are weighed on top, so `total` is more than `size`. It returns null if base or size is not positive, and
  skips lines with no material or no positive amount.
- No rounding in the math. Display rounds per unit: g 1 decimal, kg 3, lb 3, oz 2 (`formatWeight`).
- Units `g | kg | lb | oz`, with no conversion: the batch is computed in whatever unit the size is typed in.
- Batch sheets replace material names with in-house codes, so an operator can weigh a batch without seeing
  the recipe (`sheetFrom`, `codeProblem` checks codes are unique per material). That is about secrecy and
  matters less in a studio.
- No specific gravity or water math. The mix log records a mix against a sheet or recipe: dry batch size,
  unit, a lot number per line, water at the start, then dated steps of water, deflocculant, specific
  gravity, Ford cup #4 seconds and gel, as typed. `mixTotals` sums water and deflocculant and gives each as
  a percent of the dry batch size. `daysAged` counts days from mixing to a step.

### Reusable pieces

- Chemistry: still Glazecalc's JS/TS (`vendor/glazecalc/lib/chemistry`, `ui/chemistry.ts`, `limits.ts`,
  `fit.ts`). The Rust core holds no chemistry; it is storage, crypto and sync. So there is nothing new to
  port, and the earlier advice stands: call `lib/chemistry` from the web client if ever needed.
- Materials data: the same `vendor/glazecalc/data/*.ndjson` as Glazecalc. Nothing new.
- UI ideas: batch sheet screen (`ui/screens/BatchSheets.tsx`), mix log (`ui/screens/MixLog.tsx`), print
  view, the field list in `ui/recipeFields.ts` (Glazy-style options).
- License: MIT (`LICENSE`), same author, and `vendor/glazecalc/LICENSE` for the copied code.

### Recommendation from glaze-vault

- **Take the batch math and port it to Java.** It is about 20 lines: base from non-addition lines,
  `weight = amount / base * size`, additions on top, round only for display (per-unit decimals as above).
  Add a lb/kg conversion, which glaze-vault leaves out. This is the core of a studio "mix a bucket" screen.
- **Take the mix log idea for shared studio bucket glazes.** A studio glaze (owned by the studio) gets mix
  records: who mixed it, when, dry weight, water added, a specific gravity reading and notes. That is
  what a studio manager actually tracks. Store readings as decimals, not free text.
- **Separate studio bucket glazes from member recipes.** Studio glazes are staff-edited, have a target
  specific gravity and mix history, and are visible to all members. Member recipes are private by
  default, imported or typed, with no mix log. Use the same recipe and line tables with an owner kind.
- **Add `glaze-vault-export` v1 as a second import format,** behind the same preview and limits as the
  Glazecalc import. Map `name`, `lines` (addition flag to `kind = ADDITION`, `unit = PERCENT`), `cone`,
  `atmosphere`, `gravity`, `water`, `tags` and `notes`. Take a line's oxides and LOI from `materials` by
  name, into the per-line `chemistry` JSON. Insight XML is a cheap third format later, since Glazy also
  exports it.
- **Leave out:** the encryption, keyring, recovery keys, LAN sync, phone builds, coded batch sheets and
  material codes, clay-body fields, the firing log (the studio has its own kiln log), costs and blends.
- **Overall recommendation is unchanged** except for two additions: the import accepts both
  `glazecalc-export` and `glaze-vault-export` v1, and the glaze book's first feature after import is the
  batch calculator for studio bucket glazes, with a simple mix log.
