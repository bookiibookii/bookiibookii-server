package com.example.bookiibookii.domain.notification.listener;

import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.comment.service.CommentNotificationService;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.service.GroupNotificationService;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.event.ReadingCardReactionNotificationEvent;
import com.example.bookiibookii.domain.notification.service.DirectExchangeNotificationService;
import com.example.bookiibookii.domain.notification.service.KeywordNotificationService;
import com.example.bookiibookii.domain.notification.service.ReadingCardReactionNotificationService;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.tracker.service.DeliveryNotificationService;
import com.example.bookiibookii.domain.tracker.service.TrackerNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final TrackerNotificationService trackerNotificationService;
    private final KeywordNotificationService keywordNotificationService;
    private final CommentNotificationService commentNotificationService;
    private final GroupNotificationService groupNotificationService;
    private final DirectExchangeNotificationService directExchangeNotificationService;
    private final ReadingCardReactionNotificationService readingCardReactionNotificationService;
    private final DeliveryNotificationService deliveryNotificationService;

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleKeyword(KeywordGroupCreatedEvent event) {
        keywordNotificationService.send(event);
    }

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleComment(CommentEvent event) {
        commentNotificationService.send(event);
    }

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTracker(TrackerNotificationEvent event) {
        trackerNotificationService.send(event);
    }

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDelivery(DeliveryNotificationEvent event) {
        deliveryNotificationService.send(event);
    }

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroup(GroupNotificationEvent event) {
        groupNotificationService.send(event);
    }

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDirectExchange(DirectExchangeNotificationEvent event) {
        directExchangeNotificationService.send(event);
    }

    @Async("notiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReadingCardReaction(ReadingCardReactionNotificationEvent event) {
        readingCardReactionNotificationService.send(event);
    }
}
