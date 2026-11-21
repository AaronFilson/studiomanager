package io.github.aaronfilson.studiomanager.kiln;

/**
 * Controller family whose column-mapping preset was used to import a firing log.
 *
 * <p>{@link #GENESIS} and {@link #TAP} follow publicly documented controller traits (see {@code
 * docs/planning/09-kiln-log-import-research.md}) and remain marked unverified until tested against
 * real controller exports.
 */
public enum KilnLogSource {
  GENESIS(true),
  TAP(true),
  OTHER(false);

  private final boolean presetUnverified;

  KilnLogSource(boolean presetUnverified) {
    this.presetUnverified = presetUnverified;
  }

  public boolean presetUnverified() {
    return presetUnverified;
  }
}
