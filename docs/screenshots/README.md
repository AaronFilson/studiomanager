# Screenshots to capture

Capture these from the demo studio, "Demo Clay Co.", signed in as staff. Use a 1440 by 900
browser window and light mode. Save each as PNG in this folder under the name given, then switch
on the matching lines in the Screenshots section of the top-level README.

| File | Screen | Route | What should show |
|---|---|---|---|
| `today.png` | Studio today | `/studios/{id}/today` | Kilns in use, who's on duty, things to do |
| `pieces.png` | Pieces | `/studios/{id}/pieces` | Pieces at different stages, with photos |
| `kiln-load.png` | Kiln load | `/studios/{id}/kilns/{kilnId}` | A loaded firing and its queue |
| `invoice-compare-readers.png` | Invoice review | `/studios/{id}/supplies` (invoice inbox) | An invoice with "Compare readers" open |
| `admin-limits.png` | Platform admin | `/admin` | Invoice reader limits and a boost |
| `assistant-suggestions.png` | Assistant suggestions | `/studios/{id}/suggestions` | Pending changes proposed over MCP |

## Automated capture

1. Record an authenticated staff browser session once (keep the file outside the repo, never commit it):

   ```sh
   bash scripts/js.sh --filter @studiomanager/e2e exec playwright codegen --save-storage=/tmp/studiomanager-staff.json https://<web-url>
   ```

2. Capture all six screenshots into `docs/screenshots/` at 1440×900 in light mode:

   ```sh
   bash scripts/js.sh --filter @studiomanager/e2e exec node scripts/capture-screenshots.mjs --base-url https://<web-url> --studio-id <demo-studio-id> --storage-state /tmp/studiomanager-staff.json
   ```

Options: omit `--studio-id` to auto-discover "Demo Clay Co." from `/studios`, pass `--kiln-id <uuid>` to pin a specific kiln for `kiln-load.png`, pass `--only today,pieces` to recapture a subset, or pass `--skip-compare` to avoid triggering a live reader comparison call. Environment variables `SCREENSHOT_BASE_URL`, `SCREENSHOT_STUDIO_ID`, `SCREENSHOT_STORAGE_STATE`, and `SCREENSHOT_KILN_ID` are also supported.
