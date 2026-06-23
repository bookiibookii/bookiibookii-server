package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackerNotificationService {

    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;

    public void send(TrackerNotificationEvent event) {
        if (event.receiverIds() == null || event.receiverIds().isEmpty()) {
            return;
        }

        String payload = notificationFactory.toJson(payload(event));
        for (Long receiverId : event.receiverIds()) {
            if (receiverId == null || shouldSkipSelf(event, receiverId)) {
                continue;
            }
            try {
                notificationStore.save(notificationFactory.create(
                        receiverId,
                        event.notificationType() == NotificationType.TRACKER_READING_REVIEW_COMPLETED
                                ? event.actorId()
                                : null,
                        NotificationCategory.SYSTEM,
                        event.notificationType(),
                        title(event.notificationType()),
                        message(event),
                        payload,
                        dedupKey(event, receiverId)
                ));
            } catch (RuntimeException exception) {
                log.warn(
                        "Tracker notification save failed. type={}, receiverId={}, groupId={}",
                        event.notificationType(),
                        receiverId,
                        event.groupId(),
                        exception
                );
            }
        }
    }

    private boolean shouldSkipSelf(TrackerNotificationEvent event, Long receiverId) {
        if (event.notificationType() == NotificationType.TRACKER_EXCHANGE_COMPLETED
                || event.notificationType() == NotificationType.TRACKER_GROUP_FORCE_COMPLETED) {
            return false;
        }
        return receiverId.equals(event.actorId());
    }

    private NotificationPayload payload(TrackerNotificationEvent event) {
        NotificationPayload.NotificationPayloadBuilder builder = NotificationPayload.builder()
                .redirectType(redirectType(event.notificationType()))
                .groupId(event.groupId())
                .exchangeType(event.exchangeType())
                .commentId(event.commentId())
                .reviewId(event.reviewId());
        if (event.notificationType() == NotificationType.TRACKER_READING_REVIEW_COMPLETED) {
            builder.actorId(event.actorId())
                    .matchedMemberId(event.actorMatchedMemberId())
                    .exchangeRound(event.exchangeRound())
                    .bookId(event.bookId());
        }
        return builder.build();
    }

    private RedirectType redirectType(NotificationType type) {
        return type == NotificationType.TRACKER_COMMENT_CREATED
                ? RedirectType.TRACKER_COMMENT
                : type == NotificationType.TRACKER_EXCHANGE_COMPLETED
                || type == NotificationType.TRACKER_GROUP_FORCE_COMPLETED
                ? RedirectType.TRACKER_HOME
                : RedirectType.TRACKER_DETAIL;
    }

    private String title(NotificationType type) {
        return switch (type) {
            case TRACKER_READING_REVIEW_COMPLETED -> "파트너가 책 교환 준비를 마쳤어요";
            case TRACKER_PERIOD_EXTENDED -> "독서 기간이 변경됐어요";
            case TRACKER_COMMENT_CREATED -> "새로운 댓글이 달렸어요";
            case TRACKER_EXCHANGE_REVIEW_CREATED -> "파트너가 교환독서 후기를 남겼어요";
            case TRACKER_EXCHANGE_COMPLETED -> "교환독서가 종료됐어요";
            case TRACKER_GROUP_FORCE_COMPLETED -> "교환독서가 자동 종료됐어요";
            default -> throw new IllegalArgumentException("Unsupported tracker notification type: " + type);
        };
    }

    private String message(TrackerNotificationEvent event) {
        return switch (event.notificationType()) {
            case TRACKER_READING_REVIEW_COMPLETED -> String.format(
                    "%s님이 %s 독서를 마치고 후기를 남겼어요.",
                    event.actorNickname(),
                    event.bookTitle()
            );
            case TRACKER_PERIOD_EXTENDED -> String.format(
                    "%s님이 예상 독서 기간을 변경했어요.",
                    event.actorNickname()
            );
            case TRACKER_COMMENT_CREATED -> String.format(
                    "%s님이 새 댓글을 남겼어요.",
                    event.actorNickname()
            );
            case TRACKER_EXCHANGE_REVIEW_CREATED -> String.format(
                    "%s님이 이번 교환독서 후기를 작성했어요.",
                    event.actorNickname()
            );
            case TRACKER_EXCHANGE_COMPLETED -> String.format(
                    "%s 교환독서가 모두 완료됐어요. 새로운 교환독서를 시작해볼까요?",
                    event.bookTitle()
            );
            case TRACKER_GROUP_FORCE_COMPLETED ->
                    "14일 동안 파트너 후기가 작성되지 않아 교환독서가 자동 종료됐어요.";
            default -> throw new IllegalArgumentException(
                    "Unsupported tracker notification type: " + event.notificationType()
            );
        };
    }

    private String dedupKey(TrackerNotificationEvent event, Long receiverId) {
        return switch (event.notificationType()) {
            case TRACKER_READING_REVIEW_COMPLETED -> String.format(
                    "TRACKER_READING_REVIEW_COMPLETED:%d:%d:%d:%d:%s",
                    receiverId,
                    event.groupId(),
                    event.actorId(),
                    event.actorMatchedMemberId(),
                    event.exchangeRound()
            );
            case TRACKER_PERIOD_EXTENDED -> String.format(
                    "TRACKER_PERIOD_EXTENDED:%d:%d:%s",
                    receiverId,
                    event.groupId(),
                    event.periodEnd()
            );
            case TRACKER_COMMENT_CREATED -> String.format(
                    "TRACKER_COMMENT_CREATED:%d:%d:%d",
                    receiverId,
                    event.groupId(),
                    event.commentId()
            );
            case TRACKER_EXCHANGE_REVIEW_CREATED -> String.format(
                    "TRACKER_EXCHANGE_REVIEW_CREATED:%d:%d:%d",
                    receiverId,
                    event.groupId(),
                    event.reviewId()
            );
            case TRACKER_EXCHANGE_COMPLETED -> String.format(
                    "TRACKER_EXCHANGE_COMPLETED:%d:%d",
                    receiverId,
                    event.groupId()
            );
            case TRACKER_GROUP_FORCE_COMPLETED -> String.format(
                    "TRACKER_GROUP_FORCE_COMPLETED:%d:%d",
                    receiverId,
                    event.groupId()
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported tracker notification type: " + event.notificationType()
            );
        };
    }
}
