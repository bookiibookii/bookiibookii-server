package com.example.bookiibookii.domain.notification.listener;

import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.comment.service.CommentNotificationService;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.service.KeywordNotificationService;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.service.TrackerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final TrackerNotificationService trackerNotificationService;
    private final KeywordNotificationService keywordNotificationService;
    private final CommentNotificationService commentNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleKeyword(KeywordGroupCreatedEvent event) {
        keywordNotificationService.send(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleComment(CommentEvent event) {
        commentNotificationService.send(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTracker(TrackerNotificationEvent event) {
        trackerNotificationService.send(event);
    }
}
