package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupNotiType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
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

    private final MatchedMemberRepository matchedMemberRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final GroupsRepository groupsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(GroupNotificationEvent event) {
        GroupNotiType type = event.type();

        // 알림 필드 공통 조회 : actor 닉네임, 그룹(책 포함)
        String actorNickname = userRepository.findNameById(event.actorId())
                .orElseThrow(()-> new UserException(UserErrorCode.NOT_FOUND));

        Groups group = groupsRepository.findByIdWithBookAndHost(event.groupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        String bookTitle = group.getBook().getTitle();

        var vars = java.util.Map.of(
                "nickname", actorNickname,
                "bookTitle", bookTitle
        );
        String bodyMessage = templateRenderer.render(type.getBodyTemplate(), vars);

        String payload = notificationFactory.toJson(java.util.Map.of("groupId", event.groupId()));

        // 단일/다수 수신자 결정
        List<Long> receiverIds = resolveReceivers(event, group);

        for (Long receiverId : receiverIds) {
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

    private List<Long> resolveReceivers(GroupNotificationEvent event, Groups group) {
        // 단일 수신자 - 그대로 반환
        if (event.receiverId() != null) {
            return List.of(event.receiverId());
        }

        // receiverId == null 인 경우는 다수 수신자인 경우만 허용
        Long groupId = event.groupId();
        Long hostId = group.getHost().getId();

        if (event.type() == GroupNotiType.MATCH_AUTO_REJECTED) {
            List<Long> applicantIds = applicationRepository.findApplicantUserIdsByGroupIdAndStatus(groupId, ApplicationStatus.PENDING);
            List<Long> matchedIds = matchedMemberRepository.findMemberUserIdsByGroupId(groupId);

            return applicantIds.stream()
                    .filter(id -> !id.equals(hostId))
                    .filter(id -> !matchedIds.contains(id))
                    .toList();
        }

        if (event.type() == GroupNotiType.GROUP_DELETED) {
            return matchedMemberRepository.findMemberUserIdsByGroupId(groupId).stream()
                    .filter(id -> !id.equals(hostId))
                    .toList();
        }

        throw new GroupException(GroupErrorCode.RECEIVER_REQUIRED);
    }
}
