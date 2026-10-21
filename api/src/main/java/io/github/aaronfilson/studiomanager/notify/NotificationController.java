package io.github.aaronfilson.studiomanager.notify;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import io.github.aaronfilson.studiomanager.contract.NoticeRequestDto;
import io.github.aaronfilson.studiomanager.contract.NoticeResultDto;
import io.github.aaronfilson.studiomanager.contract.NotificationDto;
import io.github.aaronfilson.studiomanager.contract.NotificationKindDto;
import io.github.aaronfilson.studiomanager.contract.NotificationsApi;

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
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<List<NotificationDto>> listMyNotifications(UUID studioId) {
        return ResponseEntity.ok(notifications.mine().stream()
                .map(n -> new NotificationDto()
                        .id(n.id())
                        .kind(NotificationKindDto.valueOf(n.kind().name()))
                        .title(n.title())
                        .body(n.body())
                        .createdAt(n.createdAt().atOffset(ZoneOffset.UTC))
                        .readAt(n.readAt() == null ? null : n.readAt().atOffset(ZoneOffset.UTC)))
                .toList());
    }

    @Override
    @PreAuthorize("@studioAuthz.isMember(#studioId)")
    public ResponseEntity<Void> markNotificationRead(UUID studioId, UUID notificationId) {
        notifications.markRead(notificationId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("@studioAuthz.hasRole(#studioId, 'STAFF')")
    public ResponseEntity<NoticeResultDto> sendNotice(UUID studioId, NoticeRequestDto request) {
        return ResponseEntity.ok(new NoticeResultDto().sent(notifications.broadcast(request.getTitle(),
                request.getBody())));
    }

}
