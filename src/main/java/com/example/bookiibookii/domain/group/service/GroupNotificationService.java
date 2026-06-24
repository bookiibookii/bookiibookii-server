package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.enums.GroupNotiType;
import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotiTemplateRenderer;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupNotificationService {

    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;
    private final NotiTemplateRenderer templateRenderer;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(GroupNotificationEvent event) {
        GroupNotiType type = event.type();
        NotificationType notiType = type.getNotificationType();

        // 알림 필드 공통 조회 : actor 닉네임, 그룹(책 포함)
        String actorNickname = userRepository.findNickNameById(event.actorId())
                .orElse("알 수 없음");

        String groupTitle = event.groupTitle();

        var vars = java.util.Map.of(
                "nickname", actorNickname,
                "bookTitle", groupTitle,
                "groupTitle", groupTitle
        );
        String bodyMessage = templateRenderer.render(type.getBodyTemplate(), vars);

        String payload = notificationFactory.toJson(
                NotificationPayload.builder()
                        .redirectType(type.getRedirectType())
                        .groupId(event.groupId())
                        .requestId(event.requestId())
                        .exchangeType(event.exchangeType())
                        .build()
        );

        // 단일 || 다수 수신자
        List<Long> receivers = (event.receiverIds() != null && !event.receiverIds().isEmpty())
                ? event.receiverIds()
                : (event.receiverId() != null ? List.of(event.receiverId()) : List.of());

        if (receivers.isEmpty()) return;

        for (Long receiverId : receivers) {
            String dedupKey = dedupKey(type, event.requestId());
            notificationStore.save(
                    notificationFactory.create(
                            receiverId,
                            NotificationCategory.SYSTEM,
                            notiType,
                            type.title,
                            bodyMessage,
                            payload,
                            dedupKey
                    )
            );
        }
    }

    private String dedupKey(GroupNotiType type, Long requestId) {
        if (requestId == null) return null;
        return switch (type) {
            case JOIN_REQUESTED -> "NOTI-GRP-001:" + requestId;
            case MATCH_SUCCEEDED -> "NOTI-GRP-002:" + requestId;
            case MATCH_REJECTED -> "NOTI-GRP-003:" + requestId;
            default -> null;
        };
    }
}
