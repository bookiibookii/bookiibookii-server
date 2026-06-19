package com.example.bookiibookii.domain.tracker.scheduler;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.tracker.service.PackageDeliveryService;
import com.example.bookiibookii.domain.tracker.util.DeliveryNotificationDedupKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class DeliveryReceiveReminderScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final long REMINDER_DELAY_HOURS = 72;

    private final DeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    @Autowired
    public DeliveryReceiveReminderScheduler(
            DeliveryRepository deliveryRepository,
            NotificationRepository notificationRepository,
            DomainEventPublisher eventPublisher
    ) {
        this(deliveryRepository, notificationRepository, eventPublisher, Clock.system(KST));
    }

    DeliveryReceiveReminderScheduler(
            DeliveryRepository deliveryRepository,
            NotificationRepository notificationRepository,
            DomainEventPublisher eventPublisher,
            Clock clock
    ) {
        this.deliveryRepository = deliveryRepository;
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${scheduler.delivery-receive-reminder.cron:0 */5 * * * *}",
            zone = "Asia/Seoul"
    )
    @Transactional(readOnly = true)
    public void sendReceiveReminders() {
        LocalDateTime cutoff = LocalDateTime.ofInstant(clock.instant(), clock.getZone())
                .minusHours(REMINDER_DELAY_HOURS);

        for (Delivery delivery : deliveryRepository.findReceiveReminderCandidates(cutoff)) {
            String dedupKey = DeliveryNotificationDedupKey.create(
                    NotificationType.TRACKER_DELIVERY_RECEIVE_REMINDER,
                    delivery.getGroup().getId(),
                    delivery.getExchangeRound(),
                    delivery.getId()
            );
            Long receiverId = delivery.getReceiver().getUser().getId();
            if (notificationRepository.existsByReceiver_IdAndDedupKey(receiverId, dedupKey)) {
                continue;
            }

            eventPublisher.publish(new DeliveryNotificationEvent(
                    NotificationType.TRACKER_DELIVERY_RECEIVE_REMINDER,
                    delivery.getSender().getUser().getId(),
                    delivery.getSender().getUser().getNickName(),
                    receiverId,
                    delivery.getGroup().getId(),
                    delivery.getExchangeRound(),
                    delivery.getId(),
                    PackageDeliveryService.findDeliveredBookTitle(delivery)
            ));
        }
    }
}
