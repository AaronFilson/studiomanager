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

https://github.com/AaronFilson/glaze-vault is a different product, also MIT. It is a desktop app (Tauri 2,
with a Rust core and a React 19 UI) that keeps encrypted recipes on the user's own computer (SQLCipher)
and has no accounts or cloud. Its chemistry is a copy of Glazecalc's `lib/chemistry`, kept in
`vendor/glazecalc`. I did not read its import and export formats. It is not the source for this
import.

## Recommendation for the import

**Format to accept.** Accept the `glazecalc-export` JSON file, version 1. A member uploads the file they
downloaded from `GET /api/export`. Reject any other `format`, and any `version` above 1. Read only
`records.recipes`, and leave every other key alone: account, preferences, shelf, prices, notes, advice,
the firing logs and own materials. The line copies already carry the chemistry. As an option, accept
`recipes.csv` too, as a fallback that keeps only names and amounts. Don't call the Glazecalc API or use
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
