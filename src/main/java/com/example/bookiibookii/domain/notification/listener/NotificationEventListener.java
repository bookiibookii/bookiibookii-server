package com.example.bookiibookii.domain.notification.listener;

import com.example.bookiibookii.domain.notification.event.KeywordGroupMatchedEvent;
import com.example.bookiibookii.domain.notification.service.KeywordNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final KeywordNotificationService keywordNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(KeywordGroupMatchedEvent event) {
        keywordNotificationService.send(event);
    }
}
