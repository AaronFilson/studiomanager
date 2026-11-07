package io.github.aaronfilson.studiomanager.studio;

import java.util.UUID;

/**
 * Someone became, or stopped being, an owner of a studio (plan 21 §7). Published inside the
 * changing transaction, bound to the studio, so every owner can be told and a co-owner can't be
 * added quietly.
 *
 * @param accountId whose ownership changed
 * @param change added or removed
 * @param billingContactCleared they were the billing contact, so the remaining owners must pick one
 */
public record OwnersChanged(
    UUID studioId, UUID accountId, Change change, boolean billingContactCleared) {

  /** Which way the ownership went. */
  public enum Change {
    ADDED,
    REMOVED
  }
}
