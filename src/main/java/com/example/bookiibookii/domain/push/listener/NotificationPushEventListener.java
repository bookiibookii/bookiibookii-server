package com.example.bookiibookii.domain.push.listener;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.event.NotificationPushRequestedEvent;
import com.example.bookiibookii.domain.push.service.PushService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushEventListener {

    private static final List<String> ALLOWED_PAYLOAD_KEYS = List.of(
            "redirectType",
            "groupId",
            "exchangeType",
            "exchangeRound",
            "commentId",
            "parentCommentId",
            "deliveryId",
            "meetingId",
            "bookCardId",
            "noticeId",
            "requestId",
            "reviewId",
            "matchedMemberId",
            "bookId"
    );

    private final PushService pushService;
    private final ObjectMapper objectMapper;

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationPushRequestedEvent event) {
        try {
            pushService.sendToUser(
                    event.receiverId(),
                    new PushMessage(event.title(), event.body(), data(event))
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "Notification push processing failed. notificationId={}, receiverId={}",
                    event.notificationId(),
                    event.receiverId(),
                    exception
            );
        }
    }

    private Map<String, String> data(NotificationPushRequestedEvent event) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("notificationId", String.valueOf(event.notificationId()));
        data.put("notificationType", event.notificationType());

        if (event.payload() == null || event.payload().isBlank()) {
            return data;
        }

        try {
            JsonNode payload = objectMapper.readTree(event.payload());
            for (String key : ALLOWED_PAYLOAD_KEYS) {
                JsonNode value = payload.get(key);
                if (value != null && !value.isNull() && value.isValueNode()) {
                    data.put(key, value.asText());
                }
            }
        } catch (Exception exception) {
            log.warn("Notification push payload parsing failed. notificationId={}", event.notificationId());
        }
        return data;
    }
}
