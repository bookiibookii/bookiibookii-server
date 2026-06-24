package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.push.event.NotificationPushRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification saveAndFlush(Notification notification) {
        Notification saved = notificationRepository.saveAndFlush(notification);
        eventPublisher.publishEvent(new NotificationPushRequestedEvent(
                saved.getId(),
                saved.getReceiver().getId(),
                saved.getType().name(),
                saved.getTitle(),
                saved.getMessage(),
                saved.getPayload()
        ));
        return saved;
    }
}
