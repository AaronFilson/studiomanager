package io.github.aaronfilson.studiomanager.studio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Someone changed who may do what in a studio (plan 21 §7): a role's permissions, a member's duties
 * or role, or the billing contact. Published inside the changing transaction, next to its {@code
 * role_change_log} row, so the activity stream and webhooks can show it.
 *
 * @param target the role's key, the member's account id, or "studio"
 * @param added names (permission keys, duties, roles) the change added
 * @param removed names the change removed
 */
public record AccessChanged(
    UUID studioId,
    UUID actorAccountId,
    @Nullable UUID viaTokenId,
    AccessChangeLog.Kind kind,
    String target,
    List<String> added,
    List<String> removed,
    Instant at) {

  public AccessChanged {
    added = List.copyOf(added);
    removed = List.copyOf(removed);
  }
}
