package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.enums.TrackerAction;
import com.example.bookiibookii.domain.tracker.enums.TrackerNotiType;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
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
    private final MatchedMemberRepository matchedMemberRepository;
    private final UserRepository userRepository;
    private final GroupsRepository groupsRepository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(TrackerNotificationEvent event) {

        RoleStatus myRole = matchedMemberRepository.findRoleByGroupIdAndUserId(event.groupId(), event.actorId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.MATCHED_MEMBER_NOT_FOUND));

        Long receiverId = matchedMemberRepository
                .findTop1User_IdByGroup_GroupIdAndUser_IdNot(event.groupId(), event.actorId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.PARTNER_NOT_FOUND));

        TrackerNotiType type = resolveNotiType(event.action(), myRole);

        String nickname = userRepository.findNameById(event.actorId())
                .orElse("");

        Groups group = groupsRepository.findByIdWithBookAndHost(event.groupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        String due = (event.returnDueAt() == null) ? "" : event.returnDueAt().toLocalDate().format(DUE_FORMAT);

        var vars = java.util.Map.of(
                "nickname", nickname,
                "bookTitle", safe(group.getBook().getTitle()),
                "returnDueAt", due
        );

        String payload = notificationFactory.toJson(java.util.Map.of("groupId", event.groupId()));

        notificationRepository.save(
                notificationFactory.create(
                        receiverId,
                        NotificationType.SYSTEM,
                        type.title,
                        type.renderBody(vars),
                        payload
                )
        );
    }

    private TrackerNotiType resolveNotiType(TrackerAction action, RoleStatus actorRole) {
        return switch (action) {
            case SHIPPING_REGISTERED -> (actorRole == RoleStatus.GUEST)
                    ? TrackerNotiType.RETURN_SHIPPING_REGISTERED
                    : TrackerNotiType.SHIPPING_REGISTERED;

            case RECEIVED_CONFIRMED -> (actorRole == RoleStatus.HOST)
                    ? TrackerNotiType.EXCHANGE_FINISHED
                    : TrackerNotiType.RECEIVED_CONFIRMED;

            case READING_STARTED -> TrackerNotiType.READING_STARTED;
            case READING_FINISHED -> TrackerNotiType.READING_FINISHED;
            case EXTEND_REQUESTED -> TrackerNotiType.EXTEND_REQUESTED;
        };
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
