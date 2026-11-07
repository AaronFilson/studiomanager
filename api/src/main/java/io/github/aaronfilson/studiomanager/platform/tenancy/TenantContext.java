package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Who is calling, and in which studio, for the extent of one request or one piece of background
 * work.
 *
 * <p>Held in a {@link ScopedValue} rather than a {@code ThreadLocal}: it is immutable, bound for
 * exactly the duration of a call, and cannot leak into the next request that reuses the (virtual)
 * thread. A narrower scope, such as creating a studio, rebinds it with {@link #callAs}.
 *
 * <p>A person is bound with {@link #forAccount}; a background job with {@link #system}, which acts
 * on the job's own account and never above its {@link SystemActor#ceiling() ceiling} (ADR 16).
 *
 * @param accountId the signed-in account, or the job's system account
 * @param studioId the studio the call acts in; {@code null} outside any one studio
 * @param role the caller's role in that studio; present exactly when {@code studioId} is
 * @param kind whether a person or a background job is calling
 * @param viaTokenId the personal API token the person is calling through, by REST or MCP; {@code
 *     null} when they signed in to the app themselves. Histories note it as "via &lt;token
 *     name&gt;".
 */
public record TenantContext(
    UUID accountId,
    @Nullable UUID studioId,
    @Nullable StudioRole role,
    PrincipalKind kind,
    @Nullable UUID viaTokenId) {

  private static final ScopedValue<TenantContext> CURRENT = ScopedValue.newInstance();

  /**
   * Whether a person or a background job is calling. Postgres sees it as {@code
   * app.principal_kind}.
   */
  public enum PrincipalKind {
    PERSON,
    SYSTEM
  }

  public TenantContext {
    Objects.requireNonNull(accountId, "accountId");
    Objects.requireNonNull(kind, "kind");
    if ((studioId == null) != (role == null)) {
      throw new IllegalArgumentException("A studio and a role in it go together");
    }
    if (kind == PrincipalKind.SYSTEM) {
      SystemActor actor =
          SystemActor.byAccountId(accountId)
              .orElseThrow(
                  () -> new IllegalArgumentException("Not a system account: " + accountId));
      if (role != null && !actor.ceiling().includes(role)) {
        throw new IllegalArgumentException(
            "%s can't act above %s".formatted(actor, actor.ceiling()));
      }
      if (viaTokenId != null) {
        throw new IllegalArgumentException("A job doesn't call through an API token");
      }
    } else if (SystemActor.byAccountId(accountId).isPresent()) {
      throw new IllegalArgumentException("A system account can't act as a person: " + accountId);
    }
  }

  /** A context with no API token: a person signed in to the app, or a job. */
  public TenantContext(
      UUID accountId, @Nullable UUID studioId, @Nullable StudioRole role, PrincipalKind kind) {
    this(accountId, studioId, role, kind, null);
  }

  /** A person's context. */
  public TenantContext(UUID accountId, @Nullable UUID studioId, @Nullable StudioRole role) {
    this(accountId, studioId, role, PrincipalKind.PERSON);
  }

  public static TenantContext forAccount(UUID accountId) {
    return new TenantContext(accountId, null, null);
  }

  /** A job outside any one studio, for the cross-tenant functions that only jobs may call. */
  public static TenantContext system(SystemActor actor) {
    return new TenantContext(actor.accountId(), null, null, PrincipalKind.SYSTEM);
  }

  /** A job inside one studio, at its role ceiling. */
  public static TenantContext system(SystemActor actor, UUID studioId) {
    Objects.requireNonNull(studioId, "studioId");
    return new TenantContext(actor.accountId(), studioId, actor.ceiling(), PrincipalKind.SYSTEM);
  }

  public TenantContext inStudio(UUID studioId, StudioRole role) {
    if (kind == PrincipalKind.SYSTEM) {
      throw new IllegalStateException(
          "Bind a job to a studio with TenantContext.system(actor, studioId)");
    }
    return new TenantContext(accountId, studioId, role, kind, viaTokenId);
  }

  /** The same person, calling through the personal API token {@code tokenId}. */
  public TenantContext viaToken(UUID tokenId) {
    Objects.requireNonNull(tokenId, "tokenId");
    return new TenantContext(accountId, studioId, role, kind, tokenId);
  }

  /**
   * The studio this call acts in, for code that only runs inside one.
   *
   * @throws IllegalStateException outside any one studio
   */
  public UUID requireStudioId() {
    if (studioId == null) {
      throw new IllegalStateException("This call isn't bound to a studio");
    }
    return studioId;
  }

  public boolean isSystem() {
    return kind == PrincipalKind.SYSTEM;
  }

  public static Optional<TenantContext> current() {
    return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
  }

  public static TenantContext require() {
    return current()
        .orElseThrow(() -> new IllegalStateException("No tenant context is bound to this call"));
  }

  /**
   * Runs {@code operation} with {@code context} bound, restoring the previous binding afterwards.
   */
  public static <T, X extends Throwable> T callAs(
      TenantContext context, ScopedValue.CallableOp<T, X> operation) throws X {
    return ScopedValue.where(CURRENT, context).call(operation);
  }
}
