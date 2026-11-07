package io.github.aaronfilson.studiomanager.studio;

import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records every change to who may do what in the bound studio (plan 21 §7): the acting account, the
 * token it came through, and the value before and after. Written in the changing transaction, so a
 * change and its record commit or roll back together, and published as {@link AccessChanged}.
 */
@Component
public class AccessChangeLog {

  /** The most rows {@link #recent} returns. */
  static final int HISTORY_LIMIT = 200;

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
    BILLING_CONTACT,
    /** A custom role was made; the target is its key, the value its name. */
    ROLE_CREATED,
    /** A custom role was renamed; the target is its key, the values its names. */
    ROLE_RENAMED,
    /** A custom role was deleted; the target is its key, the value its name. */
    ROLE_DELETED
  }

  /**
   * One recorded change, as the roles page lists it.
   *
   * @param added names in {@code after} that weren't in {@code before}
   * @param removed names in {@code before} that aren't in {@code after}
   */
  public record Entry(
      UUID id,
      Instant at,
      UUID actorAccountId,
      @Nullable UUID viaTokenId,
      Kind kind,
      String target,
      List<String> added,
      List<String> removed) {}

  private final AccessChangeRepository changes;
  private final ApplicationEventPublisher events;
  private final Clock clock;

  AccessChangeLog(AccessChangeRepository changes, ApplicationEventPublisher events, Clock clock) {
    this.changes = changes;
    this.events = events;
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
    AccessChange change =
        changes.save(
            new AccessChange(
                caller.accountId(),
                caller.viaTokenId(),
                kind,
                target,
                beforeValue,
                afterValue,
                clock.instant()));
    Entry entry = toEntry(change);
    events.publishEvent(
        new AccessChanged(
            caller.requireStudioId(),
            entry.actorAccountId(),
            entry.viaTokenId(),
            kind,
            target,
            entry.added(),
            entry.removed(),
            entry.at()));
  }

  /** The bound studio's changes, newest first, cut at {@link #HISTORY_LIMIT}. */
  @Transactional(readOnly = true)
  public Capped<Entry> recent() {
    return Capped.fromFetched(
            changes.findNewestFirst(Limit.of(Capped.fetchSize(HISTORY_LIMIT))), HISTORY_LIMIT)
        .map(AccessChangeLog::toEntry);
  }

  private static Entry toEntry(AccessChange change) {
    Set<String> before = split(change.beforeValue());
    Set<String> after = split(change.afterValue());
    return new Entry(
        change.id(),
        change.at(),
        change.actorAccountId(),
        change.viaTokenId(),
        change.kind(),
        change.target(),
        after.stream().filter(name -> !before.contains(name)).toList(),
        before.stream().filter(name -> !after.contains(name)).toList());
  }

  private static Set<String> split(String value) {
    return Arrays.stream(value.split(","))
        .filter(name -> !name.isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String joined(Collection<String> names) {
    return names.stream().sorted().distinct().collect(Collectors.joining(","));
  }
}
