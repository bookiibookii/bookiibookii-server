package com.example.bookiibookii.domain.tracker.scheduler;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.tracker.service.PackageDeliveryService;
import com.example.bookiibookii.domain.tracker.util.DeliveryNotificationDedupKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class DeliveryReceiveReminderScheduler {

    private static final Duration REMINDER_DELAY = Duration.ofHours(72);

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
        this(deliveryRepository, notificationRepository, eventPublisher, Clock.systemUTC());
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
        Instant cutoff = clock.instant().minus(REMINDER_DELAY);

        for (Delivery delivery : deliveryRepository.findReceiveReminderCandidates(cutoff)) {
            try {
                processReceiveReminderCandidate(delivery);
            } catch (Exception exception) {
                log.error(
                        "Delivery receive reminder candidate processing failed. "
                                + "deliveryId={}, groupId={}, exchangeRound={}, receiverId={}",
                        delivery == null ? null : delivery.getId(),
                        delivery == null || delivery.getGroup() == null ? null : delivery.getGroup().getId(),
                        delivery == null ? null : delivery.getExchangeRound(),
                        delivery == null || delivery.getReceiver() == null
                                || delivery.getReceiver().getUser() == null
                                ? null : delivery.getReceiver().getUser().getId(),
                        exception
                );
            }
        }
    }

    private void processReceiveReminderCandidate(Delivery delivery) {
        String dedupKey = DeliveryNotificationDedupKey.create(
                NotificationType.TRACKER_DELIVERY_RECEIVE_REMINDER,
                delivery.getGroup().getId(),
                delivery.getExchangeRound(),
                delivery.getId()
        );
        Long receiverId = delivery.getReceiver().getUser().getId();
        if (notificationRepository.existsByReceiver_IdAndDedupKey(receiverId, dedupKey)) {
            return;
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
