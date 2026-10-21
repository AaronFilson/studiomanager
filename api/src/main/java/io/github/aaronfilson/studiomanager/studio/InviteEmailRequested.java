package io.github.aaronfilson.studiomanager.studio;

import java.util.UUID;

/**
 * An emailed invite should be sent, or sent again. It carries no code: {@link InviteEmails} makes
 * one when it sends, so no plain code is ever stored, not even in the outbox.
 *
 * @param requestedBy the staff member who asked for it, named in the email
 */
public record InviteEmailRequested(UUID inviteId, UUID studioId, UUID requestedBy) {
}
