package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.notification.event.ReadingCardReactionNotificationEvent;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingCardReactionNotificationService {

    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;

    public void send(ReadingCardReactionNotificationEvent event) {
        if (event.receiverId() == null || event.receiverId().equals(event.reactorId())) {
            return;
        }

        try {
            notificationStore.save(notificationFactory.create(
                    event.receiverId(),
                    event.reactorId(),
                    NotificationCategory.SYSTEM,
                    NotificationType.READING_CARD_REACTION_CREATED,
                    "파트너가 반응을 남겼어요",
                    String.format("%s님이 회원님의 독서카드에 반응을 남겼어요.", event.reactorNickname()),
                    notificationFactory.toJson(NotificationPayload.builder()
                            .redirectType(RedirectType.BOOK_CARD_DETAIL)
                            .groupId(event.groupId())
                            .cardId(event.cardId())
                            .build()),
                    String.format(
                            "%s:card:%d:reactor:%d",
                            NotificationType.READING_CARD_REACTION_CREATED.name(),
                            event.cardId(),
                            event.reactorId()
                    )
            ));
        } catch (RuntimeException exception) {
            log.warn(
                    "Reading card reaction notification save failed. cardId={}, reactorId={}, receiverId={}",
                    event.cardId(),
                    event.reactorId(),
                    event.receiverId(),
                    exception
            );
        }
    }
}
