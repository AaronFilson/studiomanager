package io.github.aaronfilson.studiomanager.platform.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param mode whether the stored permission sets decide ({@code enforcing}, the default) or only
 *     run alongside the code defaults, which decide, with each difference logged ({@code shadow}).
 *     Shadow is for the release that introduces stored sets (plan 21 §6, step 2).
 */
@ConfigurationProperties("studiomanager.permissions")
public record PermissionSettings(@DefaultValue("enforcing") Mode mode) {

  public enum Mode {
    SHADOW,
    ENFORCING
  }
}
