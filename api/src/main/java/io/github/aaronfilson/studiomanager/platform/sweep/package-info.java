/**
 * Scheduled work across studios: one copy at a time under a Postgres advisory lock, timed, and
 * runnable once by the sweep job while the API sleeps (ADR 23).
 */
@NullMarked
package io.github.aaronfilson.studiomanager.platform.sweep;

import org.jspecify.annotations.NullMarked;
