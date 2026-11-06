package io.github.aaronfilson.studiomanager.platform.sweep;

/**
 * A piece of background work that crosses studios: a retry, a reminder, a cleanup. Each one runs on
 * a schedule while a replica is up, and once per run of the sweep job (ADR 23), always through
 * {@link SweepRunner}, which makes sure only one copy runs at a time.
 */
public interface Sweep {

  /** A short, stable name: the advisory lock key, the metric tag and the log field. */
  String name();

  /** One pass. Safe to repeat: a pass that finds nothing to do does nothing. */
  void sweep();
}
