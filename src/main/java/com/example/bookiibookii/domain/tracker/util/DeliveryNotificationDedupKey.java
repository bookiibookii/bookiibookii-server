package com.example.bookiibookii.domain.tracker.util;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;

public final class DeliveryNotificationDedupKey {

    private DeliveryNotificationDedupKey() {
    }

    public static String create(
            NotificationType type,
            Long groupId,
            ExchangeRound exchangeRound,
            String deliveryId
    ) {
        String eventName = switch (type) {
            case TRACKER_SHIPMENT_REGISTERED -> "tracking-registered";
            case TRACKER_DELIVERY_CONFIRMED -> "received-confirmed";
            case TRACKER_DELIVERY_RECEIVE_REMINDER -> "receive-reminder";
            default -> throw new IllegalArgumentException("Unsupported delivery notification type: " + type);
        };
        return String.format(
                "delivery:%s:group:%d:round:%s:delivery:%s",
                eventName,
                groupId,
                exchangeRound,
                deliveryId
        );
    }
}
