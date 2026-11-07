package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records every change to who may do what in the bound studio (plan 21 §7): the acting account, the
 * token it came through, and the value before and after. Written in the changing transaction, so a
 * change and its record commit or roll back together.
 */
@Component
public class AccessChangeLog {

  /** What changed. */
  public enum Kind {
    /** A role's permission set; the target is the role's key. */
    ROLE_PERMISSIONS,
    /** A member's duties; the target is their account id. */
    MEMBER_DUTIES,
    /**
     * A member's role, such as becoming or ceasing to be an owner; the target is their account id.
     */
    MEMBER_ROLE,
    /** The studio's billing contact; the target is "studio", the values account ids. */
    BILLING_CONTACT
  }

  private final AccessChangeRepository changes;
  private final Clock clock;

  AccessChangeLog(AccessChangeRepository changes, Clock clock) {
    this.changes = changes;
    this.clock = clock;
  }

  /**
   * Records a change by the caller, if anything changed. {@code before} and {@code after} are names
   * (permission keys, duty names), stored sorted and comma-separated.
   *
   * @throws org.springframework.transaction.IllegalTransactionStateException outside a transaction
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void record(
      Kind kind, String target, Collection<String> before, Collection<String> after) {
    String beforeValue = joined(before);
    String afterValue = joined(after);
    if (beforeValue.equals(afterValue)) {
      return;
    }
    TenantContext caller = TenantContext.require();
    changes.save(
        new AccessChange(
            caller.accountId(),
            caller.viaTokenId(),
            kind,
            target,
            beforeValue,
            afterValue,
            clock.instant()));
  }

  private static String joined(Collection<String> names) {
    return names.stream().sorted().distinct().collect(Collectors.joining(","));
  }
}
