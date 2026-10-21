# Wave 2, first feature: maintenance of equipment and the building

_Started 2026-10-20 from the research in [05](05-maintenance-research.md). P1 items 4–7 (photos,
hardening, pilot kit, first deploy) are still to do; this runs alongside them._

## Decisions

- **A new `maintenance` module owns all upkeep, kilns included.** Kilns lose their single
  "every N firings" threshold, their counter of firings since maintenance, and their log. Those
  move into maintenance (V12 migrates the data). The kiln module keeps `total_firings`, which
  maintenance reads.
  - Why not keep `maintenanceDue` on kilns, as the research suggested? Kiln would then depend on
    maintenance, and maintenance on kiln. The modularity test forbids that cycle, and it is the
    tangle the module boundaries exist to stop.
- **An asset is anything that needs upkeep:** a kiln, wheel, pug mill, slab roller, extruder, clay
  trap, spray booth, ventilation, the building, safety equipment, or other.
  - Each kiln gets an asset automatically, from a `KilnCreated` event.
  - A kiln asset takes its name and its "down" state from the kiln, so they can't disagree.
    Other assets have their own.
- **A task is a recurring job on an asset.** It falls due every N days, every N firings (kiln
  assets only), or whichever comes first, and can carry a checklist.
  - It can be assigned to a duty (any technician, any monitor) or to one member.
  - "Due soon" means within a tenth of the interval: at least 1 day or 1 firing, at most 14 days.
- **Completing a task restarts only that task's clock.** That fixes the old reset, where logging
  any repair zeroed the kiln's count. A repair outside any task is logged against the asset alone.
- **Records are insert-only:** who, when, note, cost, checklist lines ticked, and the firing count
  at the time. They are the history an insurer or landlord can ask for.
- **Who can do what:**
  - Staff manage assets and tasks.
  - The assignee, whether the named member or anyone holding the duty, can complete a task.
  - Members see the tasks that are theirs.

## This slice

1. V12: `asset`, `maintenance_task` and `maintenance_record`, with RLS. Existing kilns, their
   thresholds and their logs are migrated, then the old kiln columns and table are dropped.
2. API: assets, tasks, completing a task, logging a repair, per-asset history, "my tasks".
   Today lists due tasks in place of "kilns due for maintenance".
3. Web: a Maintenance page. The kiln page links to it instead of keeping its own log.
4. Tests through the API, including the migration's behaviour and the reset fix.

## Next slices

- A daily reminder email to the assignee, once per due cycle, through the outbox.
- Photos on records (after P1 item 4), and spare parts used, drawn from supplies.
- History as CSV, and starter tasks when an asset is added (L&L's 1/10/30 firings, a weekly trap
  check, a monthly extinguisher check).
- A down asset blocking calendar bookings, and "my tasks" in the mobile app.
- Members reporting a problem from their phone.
