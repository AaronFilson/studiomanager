package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * Who is calling, and in which studio, for the extent of one request.
 *
 * <p>Held in a {@link ScopedValue} rather than a {@code ThreadLocal}: it is immutable, bound for
 * exactly the duration of a call, and cannot leak into the next request that reuses the
 * (virtual) thread. A narrower scope, such as creating a studio, rebinds it with {@link #callAs}.
 *
 * @param accountId the signed-in account
 * @param studioId the studio the request acts in; {@code null} outside any one studio
 * @param role the caller's role in that studio; present exactly when {@code studioId} is
 */
public record TenantContext(UUID accountId, @Nullable UUID studioId, @Nullable StudioRole role) {

    private static final ScopedValue<TenantContext> CURRENT = ScopedValue.newInstance();

    public TenantContext {
        Objects.requireNonNull(accountId, "accountId");
        if ((studioId == null) != (role == null)) {
            throw new IllegalArgumentException("A studio and a role in it go together");
        }
    }

    public static TenantContext forAccount(UUID accountId) {
        return new TenantContext(accountId, null, null);
    }

    public TenantContext inStudio(UUID studioId, StudioRole role) {
        return new TenantContext(accountId, studioId, role);
    }

    public static Optional<TenantContext> current() {
        return CURRENT.isBound() ? Optional.of(CURRENT.get()) : Optional.empty();
    }

    public static TenantContext require() {
        return current().orElseThrow(() -> new IllegalStateException("No tenant context is bound to this call"));
    }

    /** Runs {@code operation} with {@code context} bound, restoring the previous binding afterwards. */
    public static <T, X extends Throwable> T callAs(TenantContext context, ScopedValue.CallableOp<T, X> operation)
            throws X {
        return ScopedValue.where(CURRENT, context).call(operation);
    }

}
