package io.github.aaronfilson.studiomanager.notify;

import io.github.aaronfilson.studiomanager.platform.Capped;
import io.github.aaronfilson.studiomanager.platform.InvalidInputException;
import io.github.aaronfilson.studiomanager.platform.NotFoundException;
import io.github.aaronfilson.studiomanager.platform.tenancy.Permission;
import io.github.aaronfilson.studiomanager.platform.tenancy.StudioPermissions;
import io.github.aaronfilson.studiomanager.platform.tenancy.TenantContext;
import io.github.aaronfilson.studiomanager.studio.MemberView;
import io.github.aaronfilson.studiomanager.studio.StudioMembers;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Members' inbox in each studio, and notices from staff. Each notice is also emailed ({@link
 * NotificationEmails}), through the transactional outbox, so nothing is sent for work that rolled
 * back. Notices about what happens elsewhere are {@link NotificationListeners}.
 */
@Service
public class NotificationService {

  /** The most notifications {@link #mine} returns. */
  static final int INBOX_LIMIT = 100;

  private final NotificationRepository notifications;
  private final Notices notices;
  private final StudioMembers members;
  private final Clock clock;
  private final StudioPermissions permissions;

  NotificationService(
      NotificationRepository notifications,
      Notices notices,
      StudioMembers members,
      Clock clock,
      StudioPermissions permissions) {
    this.notifications = notifications;
    this.notices = notices;
    this.members = members;
    this.clock = clock;
    this.permissions = permissions;
  }

  /** The caller's notifications in the bound studio, newest first. */
  @Transactional(readOnly = true)
  public Capped<NotificationView> mine() {
    return Capped.fromFetched(
            notifications.findInbox(
                TenantContext.require().accountId(), Limit.of(Capped.fetchSize(INBOX_LIMIT))),
            INBOX_LIMIT)
        .map(Notification::toView);
  }

  /** Marks one of the caller's notifications read. Reading it again changes nothing. */
  @Transactional
  public void markRead(UUID notificationId) {
    notifications
        .findByIdAndAccountId(notificationId, TenantContext.require().accountId())
        .orElseThrow(() -> new NotFoundException("No such notification."))
        .markRead(clock.instant());
  }

  /**
   * A notice from staff to every member of the studio, such as a closure. Returns how many were
   * sent.
   */
  @Transactional
  public int broadcast(String title, String body) {
    permissions.require(Permission.NOTICES_SEND, "Only staff can send studio notices.");
    String cleanTitle = title.strip();
    String cleanBody = body.strip();
    if (cleanTitle.isEmpty() || cleanTitle.length() > 120 || cleanBody.length() > 2000) {
      throw new InvalidInputException(
          "A notice needs a title of 1 to 120 characters and at most 2,000 characters of text.");
    }
    List<UUID> everyone = members.list().stream().map(MemberView::accountId).toList();
    notices.send(everyone, NotificationView.Kind.STUDIO_NOTICE, cleanTitle, cleanBody);
    return everyone.size();
  }
}
