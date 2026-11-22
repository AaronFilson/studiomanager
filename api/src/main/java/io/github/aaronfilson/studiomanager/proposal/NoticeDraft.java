package io.github.aaronfilson.studiomanager.proposal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A proposed notice to every member of the studio. Approving it broadcasts through {@code
 * NotificationService.broadcast} as the staff member who approved.
 */
@Embeddable
public record NoticeDraft(
    @Column(name = "notice_title") String title, @Column(name = "notice_body") String body) {}
