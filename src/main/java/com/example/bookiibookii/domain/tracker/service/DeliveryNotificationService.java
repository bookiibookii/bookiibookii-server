package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.tracker.util.DeliveryNotificationDedupKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryNotificationService {

    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;

    public void send(DeliveryNotificationEvent event) {
        if (event.receiverId() == null || event.receiverId().equals(event.actorId())) {
            return;
        }

        try {
            notificationStore.save(notificationFactory.create(
                    event.receiverId(),
                    null,
                    NotificationCategory.SYSTEM,
                    event.notificationType(),
                    title(event.notificationType()),
                    message(event),
                    notificationFactory.toJson(payload(event)),
                    DeliveryNotificationDedupKey.create(
                            event.notificationType(),
                            event.groupId(),
                            event.exchangeRound(),
                            event.deliveryId()
                    )
            ));
        } catch (RuntimeException exception) {
            log.warn(
                    "Delivery notification save failed. type={}, receiverId={}, deliveryId={}",
                    event.notificationType(),
                    event.receiverId(),
                    event.deliveryId(),
                    exception
            );
        }
    }

    private NotificationPayload payload(DeliveryNotificationEvent event) {
        return NotificationPayload.builder()
                .redirectType(RedirectType.TRACKER_DETAIL)
                .groupId(event.groupId())
                .exchangeType(ExchangeType.DELIVERY)
                .exchangeRound(event.exchangeRound())
                .deliveryId(event.deliveryId())
                .build();
    }

    private String title(NotificationType type) {
        return switch (type) {
            case TRACKER_SHIPMENT_REGISTERED -> "책이 오고 있어요!";
            case TRACKER_DELIVERY_CONFIRMED -> "내가 보낸 책이 안전하게 도착했어요";
            case TRACKER_DELIVERY_RECEIVE_REMINDER -> "책을 받으셨나요?";
            default -> throw new IllegalArgumentException("Unsupported delivery notification type: " + type);
        };
    }

    private String message(DeliveryNotificationEvent event) {
        return switch (event.notificationType()) {
            case TRACKER_SHIPMENT_REGISTERED -> String.format(
                    "%s님이 %s을 발송했어요. 운송장 번호를 확인해보세요.",
                    event.actorNickname(), event.bookTitle()
            );
            case TRACKER_DELIVERY_CONFIRMED -> String.format(
                    "%s님이 %s을 잘 받았다고 해요!",
                    event.actorNickname(), event.bookTitle()
            );
            case TRACKER_DELIVERY_RECEIVE_REMINDER -> String.format(
                    "%s님이 %s을 보낸 지 3일이 지났어요. 책을 받으셨다면 수령 확인을 눌러주세요.",
                    event.actorNickname(), event.bookTitle()
            );
            default -> throw new IllegalArgumentException(
                    "Unsupported delivery notification type: " + event.notificationType()
            );
        };
    }
}
