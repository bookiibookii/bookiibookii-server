package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStore {

    private static final String DEDUP_UNIQUE_CONSTRAINT = "uk_notification_receiver_dedup_key";

    private final NotificationPersistenceService persistenceService;

    public Optional<Notification> save(Notification notification) {
        try {
            return Optional.of(persistenceService.saveAndFlush(notification));
        } catch (DataIntegrityViolationException e) {
            Long receiverId = notification.getReceiver().getId();
            String dedupKey = notification.getDedupKey();
            if (dedupKey == null || !isDedupConstraintViolation(e)) {
                throw e;
            }
            log.debug(
                    "Duplicate notification ignored. receiverId={}, dedupKey={}",
                    receiverId,
                    dedupKey
            );
            return Optional.empty();
        }
    }

    public List<Notification> saveAll(List<Notification> notifications) {
        if (notifications.isEmpty()) return List.of();
        try {
            return persistenceService.saveAllAndFlush(notifications);
        } catch (DataIntegrityViolationException e) {
            if (!isDedupConstraintViolation(e)) throw e;
            return notifications.stream()
                    .map(this::copyForRetry)
                    .map(this::save)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
    }

    private Notification copyForRetry(Notification notification) {
        // A rolled-back IDENTITY insert can leave an ID on the entity without a database row.
        // Retry with a fresh entity so JPA inserts it instead of merging the rolled-back ID.
        return Notification.builder()
                .receiver(notification.getReceiver())
                .actor(notification.getActor())
                .category(notification.getCategory())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .payload(notification.getPayload())
                .dedupKey(notification.getDedupKey())
                .read(notification.isRead())
                .readAt(notification.getReadAt())
                .build();
    }

    private boolean isDedupConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                if (constraintName != null
                        && constraintName.toLowerCase().contains(DEDUP_UNIQUE_CONSTRAINT)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
