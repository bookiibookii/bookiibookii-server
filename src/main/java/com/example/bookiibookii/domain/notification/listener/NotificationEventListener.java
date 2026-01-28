package com.example.bookiibookii.domain.notification.listener;

import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.comment.service.CommentNotificationService;
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
    private final CommentNotificationService commentNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleKeyword(KeywordGroupMatchedEvent event) {
        keywordNotificationService.send(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleComment(CommentEvent event) {
        commentNotificationService.send(event);
    }
}
