package io.github.aaronfilson.studiomanager.ratelimit;

import java.util.UUID;

/**
 * The limits from configuration, the same for every studio and never boosted. The admin module's
 * database-backed settings replace this bean once they exist (see {@link RateLimitConfiguration}).
 */
class ConfiguredRateLimitSettings implements RateLimitSettings {

  /** Configuration only changes with a restart, so its limits keep one revision. */
  private static final long REVISION = 0;

  private final EffectiveLimit.Limited mcp;
  private final EffectiveLimit.Limited token;

  ConfiguredRateLimitSettings(RateLimitProperties properties) {
    this.mcp = new EffectiveLimit.Limited(properties.mcp(), REVISION);
    this.token = new EffectiveLimit.Limited(properties.token(), REVISION);
  }

  @Override
  public EffectiveLimit current(Channel channel, UUID studioId) {
    return switch (channel) {
      case MCP -> mcp;
      case TOKEN -> token;
    };
  }
}
