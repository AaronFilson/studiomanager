package io.github.aaronfilson.studiomanager.platform.tenancy;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * A background job that acts in studios on its own account, not a person's (ADR 16).
 *
 * <p>Each has a seeded {@code account} row of kind {@code SYSTEM} with the same id (V25), so
 * anything it writes names it, and a role ceiling: the most it may do in a studio. None may act as
 * an owner. A new job adds a constant here and a row in a migration.
 */
public enum SystemActor {
  ACTIVITY_CONSUMER("5157e000-0000-4000-8000-000000000001", "activity-consumer", StudioRole.STAFF),
  MAINTENANCE_REMINDERS(
      "5157e000-0000-4000-8000-000000000002", "maintenance-reminders", StudioRole.STAFF),
  WEBHOOK_SWEEP("5157e000-0000-4000-8000-000000000003", "webhook-sweep", StudioRole.STAFF),
  UNCLAIMED_PIECES("5157e000-0000-4000-8000-000000000004", "unclaimed-pieces", StudioRole.STAFF),
  PHOTO_CLEANUP("5157e000-0000-4000-8000-000000000005", "photo-cleanup", StudioRole.STAFF);

  /** The issuer on every system account. No identity provider issues it, so no token can match. */
  public static final String ISSUER = "urn:studiomanager:system";

  private final UUID accountId;
  private final String subject;
  private final StudioRole ceiling;

  SystemActor(String accountId, String subject, StudioRole ceiling) {
    if (ceiling.includes(StudioRole.OWNER)) {
      throw new IllegalArgumentException("A system actor can't act as an owner");
    }
    this.accountId = UUID.fromString(accountId);
    this.subject = subject;
    this.ceiling = ceiling;
  }

  public static Optional<SystemActor> byAccountId(UUID accountId) {
    return Arrays.stream(values()).filter(actor -> actor.accountId.equals(accountId)).findFirst();
  }

  public UUID accountId() {
    return accountId;
  }

  public String subject() {
    return subject;
  }

  public StudioRole ceiling() {
    return ceiling;
  }
}
