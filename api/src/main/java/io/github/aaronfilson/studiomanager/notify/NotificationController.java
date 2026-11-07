package io.github.aaronfilson.studiomanager.notify;

import io.github.aaronfilson.studiomanager.contract.NoticeRequestDto;
import io.github.aaronfilson.studiomanager.contract.NoticeResultDto;
import io.github.aaronfilson.studiomanager.contract.NotificationDto;
import io.github.aaronfilson.studiomanager.contract.NotificationKindDto;
import io.github.aaronfilson.studiomanager.contract.NotificationListDto;
import io.github.aaronfilson.studiomanager.contract.NotificationsApi;
import io.github.aaronfilson.studiomanager.platform.Capped;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NotificationController implements NotificationsApi {

  private final NotificationService notifications;

  NotificationController(NotificationService notifications) {
    this.notifications = notifications;
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'NOTICES_READ')")
  public ResponseEntity<NotificationListDto> listMyNotifications(UUID studioId) {
    Capped<NotificationDto> inbox =
        notifications
            .mine()
            .map(n ->
                new NotificationDto()
                    .id(n.id())
                    .kind(NotificationKindDto.valueOf(n.kind().name()))
                    .title(n.title())
                    .body(n.body())
                    .createdAt(n.createdAt().atOffset(ZoneOffset.UTC))
                    .readAt(n.readAt() == null ? null : n.readAt().atOffset(ZoneOffset.UTC)));
    return ResponseEntity.ok(
        new NotificationListDto(inbox.items(), inbox.truncated(), inbox.limit()));
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'NOTICES_READ')")
  public ResponseEntity<Void> markNotificationRead(UUID studioId, UUID notificationId) {
    notifications.markRead(notificationId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("@studioAuthz.can(#studioId, 'NOTICES_SEND')")
  public ResponseEntity<NoticeResultDto> sendNotice(UUID studioId, NoticeRequestDto request) {
    return ResponseEntity.ok(
        new NoticeResultDto().sent(notifications.broadcast(request.getTitle(), request.getBody())));
  }
}
