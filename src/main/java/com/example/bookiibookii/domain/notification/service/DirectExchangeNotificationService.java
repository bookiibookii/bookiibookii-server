package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.resolver.ActiveExchangeRoundResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectExchangeNotificationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;
    private final MeetingRepository meetingRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final ActiveExchangeRoundResolver activeExchangeRoundResolver;

    public void send(DirectExchangeNotificationEvent event) {
        if (event.receiverId() == null || isSelfNotification(event) || !isCurrentlyEligible(event)) {
            return;
        }

        try {
            notificationStore.save(notificationFactory.create(
                    event.receiverId(),
                    event.actorId(),
                    NotificationCategory.SYSTEM,
                    event.notificationType(),
                    title(event.notificationType()),
                    message(event),
                    notificationFactory.toJson(NotificationPayload.builder()
                            .redirectType(RedirectType.TRACKER_DETAIL)
                            .groupId(event.groupId())
                            .exchangeType(ExchangeType.DIRECT)
                            .exchangeRound(event.exchangeRound())
                            .build()),
                    dedupKey(event)
            ));
        } catch (RuntimeException exception) {
            log.warn(
                    "Direct exchange notification save failed. type={}, receiverId={}, groupId={}",
                    event.notificationType(),
                    event.receiverId(),
                    event.groupId(),
                    exception
            );
        }
    }

    private boolean isSelfNotification(DirectExchangeNotificationEvent event) {
        return event.actorId() != null && event.actorId().equals(event.receiverId());
    }

    private boolean isCurrentlyEligible(DirectExchangeNotificationEvent event) {
        if (event.notificationType() != NotificationType.DIRECT_MEETING_CONFIRM_REMINDER) {
            return true;
        }
        Meeting meeting = meetingRepository.findByGroupIdAndExchangeRound(
                        event.groupId(),
                        event.exchangeRound()
                )
                .orElse(null);
        if (meeting == null
                || meeting.getGroup().getTradeType() != TradeType.DIRECT
                || meeting.getGroup().getGroupStatus() != GroupStatus.MATCHED
                || meeting.getExchangeRound() != event.exchangeRound()
                || !meeting.getScheduledAt().equals(event.meetingAt())
                || meeting.getScheduledAt().isAfter(LocalDateTime.now(KST).minusHours(1))) {
            return false;
        }

        List<MatchedMember> members = matchedMemberRepository.findAllByGroup_Id(event.groupId());
        if (activeExchangeRoundResolver.resolve(members)
                .filter(event.exchangeRound()::equals)
                .isEmpty()) {
            return false;
        }

        return members.stream()
                .filter(member -> member.getUser().getId().equals(event.receiverId()))
                .anyMatch(member -> member.getExchangeStatus() == ExchangeStatus.MEETING_SCHEDULED);
    }

    private String title(NotificationType type) {
        return switch (type) {
            case DIRECT_MEETING_CREATED -> "교환 약속이 잡혔어요";
            case DIRECT_MEETING_UPDATED -> "교환 약속이 변경됐어요";
            case DIRECT_MEETING_CONFIRM_REMINDER -> "교환 확인이 필요해요";
            default -> throw new IllegalArgumentException("Unsupported direct exchange notification type: " + type);
        };
    }

    private String message(DirectExchangeNotificationEvent event) {
        return switch (event.notificationType()) {
            case DIRECT_MEETING_CREATED -> String.format(
                    "%s님이 교환 약속을 등록했어요. 일시와 장소를 확인해주세요.",
                    event.actorNickname()
            );
            case DIRECT_MEETING_UPDATED -> String.format(
                    "%s님이 교환 약속을 변경했어요. 새 일시와 장소를 확인해주세요.",
                    event.actorNickname()
            );
            case DIRECT_MEETING_CONFIRM_REMINDER -> String.format(
                    "%s 교환 약속 시간이 지났어요. 교환을 완료하셨다면 확인해주세요.",
                    event.bookTitle()
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported direct exchange notification type: " + event.notificationType()
            );
        };
    }

    private String dedupKey(DirectExchangeNotificationEvent event) {
        if (event.notificationType() == NotificationType.DIRECT_MEETING_UPDATED) {
            Long meetingId = Objects.requireNonNull(event.meetingId(), "meetingId is required for meeting update");
            UUID eventId = Objects.requireNonNull(event.eventId(), "eventId is required for meeting update");
            return String.format(
                    "%s:group:%d:round:%s:receiver:%d:meeting:%d:event:%s",
                    event.notificationType().name(),
                    event.groupId(),
                    event.exchangeRound(),
                    event.receiverId(),
                    meetingId,
                    eventId
            );
        }

        String base = String.format(
                "%s:group:%d:round:%s:receiver:%d:meetingAt:%s",
                event.notificationType().name(),
                event.groupId(),
                event.exchangeRound(),
                event.receiverId(),
                event.meetingAt()
        );
        return base;
    }
}
