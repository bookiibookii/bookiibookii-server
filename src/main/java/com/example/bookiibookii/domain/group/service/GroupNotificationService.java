package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.enums.GroupNotiType;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.notification.util.NotiTemplateRenderer;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;
    private final NotiTemplateRenderer templateRenderer;

    private final UserRepository userRepository;
    private final GroupsRepository groupsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(GroupNotificationEvent event) {
        GroupNotiType type = event.type();

        // 알림 필드 공통 조회 : actor 닉네임, 그룹(책 포함)
        String actorNickname = userRepository.findNameById(event.actorId())
                .orElseThrow(()-> new UserException(UserErrorCode.NOT_FOUND));

        String bookTitle = event.bookTitle();

        var vars = java.util.Map.of(
                "nickname", actorNickname,
                "bookTitle", bookTitle
        );
        String bodyMessage = templateRenderer.render(type.getBodyTemplate(), vars);

        String payload = notificationFactory.toJson(java.util.Map.of("groupId", event.groupId()));

        // 단일 || 다수 수신자
        List<Long> receivers = (event.receiverIds() != null && !event.receiverIds().isEmpty())
                ? event.receiverIds()
                : (event.receiverId() != null ? List.of(event.receiverId()) : List.of());

        if (receivers.isEmpty()) return;

        for (Long receiverId : receivers) {
            notificationRepository.save(
                    notificationFactory.create(
                            receiverId,
                            NotificationType.SYSTEM,
                            type.title,
                            bodyMessage,
                            payload
                    )
            );
        }
    }
}
