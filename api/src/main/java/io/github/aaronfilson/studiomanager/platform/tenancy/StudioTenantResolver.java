package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Tells Hibernate which studio a session works in, for entities with a {@code @TenantId} (ADR 21).
 *
 * <p>Hibernate then sets {@code studio_id} on every insert and adds {@code studio_id = ?} to every
 * query of those entities, so the application checks the tenant as well as row-level security. Like
 * the RLS settings, it's read when the transaction's session opens, so the studio has to be bound
 * before the transaction starts.
 *
 * <p>With no studio bound ("my studios", the sweeps that find studios) it returns {@link
 * #NO_STUDIO}, which matches no row. The entities read there ({@code Account}, {@code Studio},
 * {@code StudioMembership}) have no {@code @TenantId}, so they're unaffected.
 */
final class StudioTenantResolver implements CurrentTenantIdentifierResolver<UUID> {

  /** The nil UUID. Studio ids are UUIDv7, so no studio ever has it. */
  static final UUID NO_STUDIO = new UUID(0L, 0L);

  @Override
  public UUID resolveCurrentTenantIdentifier() {
    TenantContext context = TenantContext.current().orElse(null);
    UUID studioId = context == null ? null : context.studioId();
    return studioId == null ? NO_STUDIO : studioId;
  }

  /**
   * Spring opens and closes the sessions, and never through {@code getCurrentSession()}, so there
   * is no current session to check.
   */
  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
