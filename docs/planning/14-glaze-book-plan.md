# 14. Glaze book

_Planned 2026-11-05, after plan 12 (PR #17). This is item 5 in [08](08-wave2-queue.md). The
research is in [13](13-glaze-book-research.md)._

## Why

The studio runs a few shared bucket glazes, and members keep their own recipes somewhere else.
That somewhere is often a notebook, glazecalcapp.com or glaze-vault. A studio manager gets asked
the same three questions every week:
- Which glaze is in that bucket, and is it food safe?
- How do I mix a fresh 10 kg batch?
- Who mixed this last, and why is it so thin?

The glaze book answers all three. A member's import brings their own recipes in, so they don't
retype them.

## Scope

**In:**
1. **Studio glazes.** Shared bucket glazes, visible to every member and edited by staff. Each has
   a target specific gravity, a cone and atmosphere, a food-surface flag, test-tile photos, and a
   mix log.
2. **Member recipes.** Private to the member by default. A member can share one with the studio,
   and staff can then promote it to a studio glaze.
3. **Batch calculator.** Scale any recipe to a dry batch size in g, kg, lb or oz. It's ported from
   glaze-vault's `scaleBatch`, with unit conversion added.
4. **Mix log.** Record each studio-glaze mix: who mixed it, when, the dry weight, the water, the
   specific gravity readings and notes. It warns when a reading is off the target.
5. **Import.** `glazecalc-export` v1 and `glaze-vault-export` v1 JSON go through a preview, then
   all or nothing (the same pattern as the CSV importer, item 2 in 08).

**Out:** chemistry and the unity formula, limit checks, costs, line blends, Glazy and Insight XML,
OAuth to glazecalc, and per-line material codes. The firing log stays in the kiln module.

## Design

### Module
There's a new Modulith module, `glaze`. It depends on `studio`, `account` and the photo upload
support (ADR 12), and nothing depends on it. The module test checks the boundary.

### Data (V26)
- **`glaze_recipe`:** `studio_id`, `owner_kind` (`STUDIO` | `MEMBER`), and `owner_account_id`
  (null for a studio glaze). Then `name`, `notes`, `cone` and `atmosphere` (the studio's enums,
  with the text from the import kept in `cone_text`), `food_surface`, `target_sg numeric(4,3)`,
  `shared_with_studio`, `version`, and `created_by`/`updated_by`.
  - **Source columns:** `source` (`MANUAL` | `GLAZECALC` | `GLAZE_VAULT`), `source_ref`,
    `source_rev` and `source_updated_at`.
  - **Uniqueness:** `UNIQUE (studio_id, owner_account_id, source, source_ref)` lets a re-import
    find its earlier copy.
- **`glaze_line`:** `recipe_id`, `position`, `kind` (`BASE` | `ADDITION`), `material`,
  `amount numeric(10,4)`, `amount_text` (exactly as it was written), `unit` (`PARTS` | `PERCENT` |
  `GRAMS`), and `chemistry jsonb` (copied from the source and never edited).
- **`glaze_mix`:** `recipe_id`, which must be a studio glaze, `mixed_by`, `mixed_at`,
  `dry_weight_g numeric`, `water_g numeric`, and `notes`.
  - **`glaze_mix_reading`:** `mix_id`, `read_at`, `specific_gravity numeric(4,3)`, and
    `note`.
- **`glaze_photo`:** the same shape as piece photos, with the same direct-to-blob flow and the
  same abandoned-upload sweep. That sweep acts as `PHOTO_CLEANUP` (ADR 16), and its definer
  function is extended.
- **RLS:**
  - The tenant policy covers all of it.
  - A member recipe is also limited to its owner unless it's shared:
    `owner_kind = 'STUDIO' OR owner_account_id = app.current_account_id() OR shared_with_studio`.
  - Staff can read shared recipes but can't edit someone else's.

### Batch math (Java, `glaze.BatchCalculator`)
- The base is the sum of the `BASE` lines. If every line is an addition, the base is all of them.
- `weight = amount / base × size`. A `PERCENT` addition is `amount / 100 × size`, weighed on top.
  A `GRAMS` addition stays fixed, converted into the batch's unit.
- Units: `g`, `kg`, `lb`, `oz`. The math runs in grams, converts at the end with `BigDecimal`, and
  rounds only for display (g 1 decimal, kg and lb 3, oz 2).
- It's a pure function, unit-tested with property tests (jqwik): scaling is linear, the base lines
  add up to the size, and converting a unit there and back gives the same answer.
- Endpoint: `GET /glazes/{id}/batch?size=10&unit=kg`. The web screen recalculates as the member
  types and prints a batch sheet.

### Import
- `POST /glazes/import?dryRun=true` takes the file, up to 2 MB, 500 recipes and 60 lines per
  recipe.
  - The format is detected from `format` and `version`. Anything else is rejected with a clear
    problem response (RFC 9457, like the rest of the API).
  - The parsing is strict: Jackson with unknown properties ignored. Only `records.recipes` (from
    glazecalc) or `recipes` plus `materials` (from glaze-vault) are read.
  - Notes are stripped to plain text.
- **Glazecalc mapping:** as in 13's table. Additive units carry over as they were.
- **glaze-vault mapping:**
  - An `addition` line becomes `ADDITION` with unit `PERCENT`.
  - Chemistry comes from `materials`, matched by name.
  - `gravity` becomes `target_sg`, but it's ignored on a member recipe.
  - Dedup uses `source_ref = name`. The preview says so.
- **Preview:** each row shows new, update (the source copy is newer), unchanged or error, with
  warnings for an unknown cone, an unparseable amount or a material with no chemistry. Then
  `dryRun=false` runs the same import for real, in one transaction.
- Imports always create member recipes, owned by the member who imported them.

### API and clients
- OpenAPI first:
  - `GlazeRecipe`, `GlazeLine`, `GlazeMix`, `BatchSheet` and `GlazeImportResult`;
  - `/glazes` (with the filters `owner=studio|me|shared`, `cone` and `food`), `/glazes/{id}`,
    `/glazes/{id}/mixes`, `/glazes/{id}/batch`, `/glazes/{id}/promote` and
    `/glazes/{id}/photos`.
- **Web:** a "Glaze book" page with the studio and mine tabs, a recipe page, the batch calculator,
  the mix log with a small SG chart, and an import wizard.
- **Mobile:** studio glazes read-only, with the batch calculator and "log a mix". A phone is what
  you have at the bucket.
- **Webhook:** `glaze.mixed`, through the existing outbox. It also streams to Kafka when that's on.

### Promote
`POST /glazes/{id}/promote` is for staff, on a shared member recipe. It copies the recipe into a
new studio glaze with `source = MANUAL`, and keeps a `promoted_from` link. The member keeps their
own copy.

## Steps
1. V26 with RLS, the module, CRUD for studio and member recipes, the OpenAPI spec and the client,
   the web list and recipe page, and tests (including RLS between members).
2. The batch calculator, its property tests, and the web and mobile screens.
3. The mix log and readings, the SG warning, the `glaze.mixed` webhook and the web chart.
4. Import for both formats, with the preview, the limits and fixtures from the real exports
   (ask the owner for a sample from each).
5. Photos and promote. Then ADR 17 (the glaze book: member-private recipes under studio RLS, and
   import as untrusted input), the README and 08's status.

## Tests
- **Unit:** the batch math (examples plus properties), each import mapper against fixture files,
  and the limits.
- **Integration:**
  - A member can't see another member's unshared recipe, and staff can't edit it.
  - A re-import updates the recipe and doesn't duplicate it.
  - A dry run writes nothing.
  - An oversized file returns 413 or a problem response.
  - The photo sweep cleans abandoned glaze uploads.
- **Web:** the import wizard, the batch calculator and the mix log, with MSW.
- **Mobile:** the batch screen and logging a mix.

## In an interview
"Members bring recipes from two other apps I wrote, so the import treats every file as untrusted:
it detects the format, enforces size and count limits, does a dry-run preview, then commits all or
nothing. Recipes can be private to one member inside a studio's tenant, so I layered an owner rule
on top of the tenant RLS policy. The batch math is a pure function with property-based tests,
because a wrong gram count ruins a bucket of glaze."
