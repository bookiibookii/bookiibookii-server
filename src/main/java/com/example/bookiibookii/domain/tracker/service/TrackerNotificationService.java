package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.notification.util.NotiTemplateRenderer;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.enums.TrackerNotiType;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackerNotificationService {

    private static final java.time.format.DateTimeFormatter DUE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("yy.MM.dd");

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;
    private final NotiTemplateRenderer templateRenderer;

    private final MatchedMemberRepository matchedMemberRepository;
    private final UserRepository userRepository;
    private final GroupsRepository groupsRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(TrackerNotificationEvent event) {

        Long receiverId = matchedMemberRepository
                .findPartnerUserId(event.groupId(), event.actorId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.PARTNER_NOT_FOUND));

        TrackerNotiType type = event.notiType();
        NotificationType notiType = type.getNotificationType();

        // 알림 필드
        String nickname = userRepository.findNickNameById(event.actorId())
                .orElseThrow(()->new UserException(UserErrorCode.NOT_FOUND));

        Groups group = groupsRepository.findByIdWithBookAndHost(event.groupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        String bookTitle = group.getBook().getTitle();

        String due = (event.returnDueAt() == null) ? "" : event.returnDueAt().toLocalDate().format(DUE_FORMAT);

        var vars = java.util.Map.of(
                "nickname", nickname,
                "bookTitle", bookTitle,
                "returnDueAt", due
        );
        String bodyMessage = templateRenderer.render(type.getBodyTemplate(), vars);

        String payload = notificationFactory.toJson(java.util.Map.of("groupId", event.groupId()));

        notificationRepository.save(
                notificationFactory.create(
                        receiverId,
                        NotificationCategory.SYSTEM,
                        notiType,
                        type.title,
                        bodyMessage,
                        payload
                )
        );
    }

}
