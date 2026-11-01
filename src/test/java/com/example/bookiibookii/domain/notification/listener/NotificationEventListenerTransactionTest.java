package com.example.bookiibookii.domain.notification.listener;

import com.example.bookiibookii.domain.comment.service.CommentNotificationService;
import com.example.bookiibookii.domain.group.service.GroupNotificationService;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.event.ReadingCardReactionNotificationEvent;
import com.example.bookiibookii.domain.notification.service.DirectExchangeNotificationService;
import com.example.bookiibookii.domain.notification.service.KeywordNotificationService;
import com.example.bookiibookii.domain.notification.service.ReadingCardReactionNotificationService;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.tracker.service.DeliveryNotificationService;
import com.example.bookiibookii.domain.tracker.service.TrackerNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig(NotificationEventListenerTransactionTest.Config.class)
class NotificationEventListenerTransactionTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TrackerNotificationService trackerNotificationService;
    @Autowired
    private KeywordNotificationService keywordNotificationService;
    @Autowired
    private CommentNotificationService commentNotificationService;
    @Autowired
    private GroupNotificationService groupNotificationService;
    @Autowired
    private DirectExchangeNotificationService directExchangeNotificationService;
    @Autowired
    private ReadingCardReactionNotificationService readingCardReactionNotificationService;
    @Autowired
    private DeliveryNotificationService deliveryNotificationService;

    @BeforeEach
    void clearMockInvocations() {
        clearInvocations(
                trackerNotificationService,
                keywordNotificationService,
                commentNotificationService,
                groupNotificationService,
                directExchangeNotificationService,
                readingCardReactionNotificationService,
                deliveryNotificationService
        );
    }

    @Test
    void handlesDeliveryEventOnlyAfterCommit() {
        DeliveryNotificationEvent event = deliveryEvent();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            verify(deliveryNotificationService, never()).send(event);
        });

        verify(deliveryNotificationService).send(event);
    }

    @Test
    void doesNotHandleDeliveryEventAfterRollback() {
        DeliveryNotificationEvent event = deliveryEvent();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(deliveryNotificationService, never()).send(event);
    }

    @Test
    void handlesDirectExchangeEventOnlyAfterCommit() {
        DirectExchangeNotificationEvent event = event();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            verify(directExchangeNotificationService, never()).send(event);
        });

        verify(directExchangeNotificationService).send(event);
    }

    @Test
    void doesNotHandleDirectExchangeEventAfterRollback() {
        DirectExchangeNotificationEvent event = event();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(directExchangeNotificationService, never()).send(event);
    }

    @Test
    void handlesReadingCardReactionOnlyAfterCommit() {
        ReadingCardReactionNotificationEvent event =
                new ReadingCardReactionNotificationEvent(2L, "파트너", 1L, 10L, 15L, 20L);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            verify(readingCardReactionNotificationService(), never()).send(event);
        });

        verify(readingCardReactionNotificationService()).send(event);
    }

    @Test
    void doesNotHandleReadingCardReactionAfterRollback() {
        ReadingCardReactionNotificationEvent event =
                new ReadingCardReactionNotificationEvent(2L, "파트너", 1L, 10L, 15L, 20L);
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(readingCardReactionNotificationService(), never()).send(event);
    }

    private ReadingCardReactionNotificationService readingCardReactionNotificationService() {
        return readingCardReactionNotificationService;
    }

    private DirectExchangeNotificationEvent event() {
        return new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_CREATED,
                1L,
                "호스트",
                2L,
                10L,
                100L,
                ExchangeRound.FIRST_EXCHANGE,
                Instant.parse("2026-06-20T05:00:00Z"),
                null,
                "교환 책"
        );
    }

    private DeliveryNotificationEvent deliveryEvent() {
        return new DeliveryNotificationEvent(
                NotificationType.TRACKER_SHIPMENT_REGISTERED,
                1L,
                "발송자",
                2L,
                10L,
                ExchangeRound.FIRST_EXCHANGE,
                "delivery-1",
                "교환 책"
        );
    }

    @Configuration
    @EnableTransactionManagement
    @Import(NotificationEventListener.class)
    static class Config {

        @Bean
        TrackerNotificationService trackerNotificationService() {
            return mock(TrackerNotificationService.class);
        }

        @Bean
        KeywordNotificationService keywordNotificationService() {
            return mock(KeywordNotificationService.class);
        }

        @Bean
        CommentNotificationService commentNotificationService() {
            return mock(CommentNotificationService.class);
        }

        @Bean
        GroupNotificationService groupNotificationService() {
            return mock(GroupNotificationService.class);
        }

        @Bean
        DirectExchangeNotificationService directExchangeNotificationService() {
            return mock(DirectExchangeNotificationService.class);
        }

        @Bean
        ReadingCardReactionNotificationService readingCardReactionNotificationService() {
            return mock(ReadingCardReactionNotificationService.class);
        }

        @Bean
        DeliveryNotificationService deliveryNotificationService() {
            return mock(DeliveryNotificationService.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
