package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.push.event.NotificationPushRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Notification> saveAllAndFlush(List<Notification> notifications) {
        List<Notification> saved = notificationRepository.saveAllAndFlush(notifications);
        saved.forEach(n -> eventPublisher.publishEvent(new NotificationPushRequestedEvent(
                n.getId(),
                n.getReceiver().getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.getPayload()
        )));
        return saved;
    }
}
