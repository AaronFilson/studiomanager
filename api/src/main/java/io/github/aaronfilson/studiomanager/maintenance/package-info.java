/**
 * Upkeep of equipment and the building: assets, the recurring tasks on them, the record of work
 * done, problem reports and record photos.
 *
 * <p>On Hibernate (ADR 21), with the studio's SQL in one class, {@code MaintenanceQueries}: the
 * reminder claim, the export, the record lock and the two cross-tenant sweeps.
 */
@NullMarked
package io.github.aaronfilson.studiomanager.maintenance;

import org.jspecify.annotations.NullMarked;
