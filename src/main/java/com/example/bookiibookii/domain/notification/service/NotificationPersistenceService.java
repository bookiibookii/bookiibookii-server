package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification saveAndFlush(Notification notification) {
        return notificationRepository.saveAndFlush(notification);
    }
}
