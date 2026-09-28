package com.example.bookiibookii.domain.tracker.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryNotificationServiceTest {

    @Mock
    private NotificationStore notificationStore;
    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DeliveryNotificationService service;
    private Logger serviceLogger;
    private Level originalLogLevel;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        lenient().when(notificationStore.save(any())).thenAnswer(invocation ->
                Optional.of(invocation.getArgument(0)));
        service = new DeliveryNotificationService(
                notificationStore,
                new NotificationFactory(userRepository, objectMapper)
        );
    }

    @AfterEach
    void tearDown() {
        if (logAppender != null) {
            serviceLogger.detachAppender(logAppender);
            logAppender.stop();
            serviceLogger.setLevel(originalLogLevel);
        }
    }

    @Test
    void storesShipmentRegistrationForDeliveryReceiverWithMinimalPayload() throws Exception {
        service.send(event(NotificationType.TRACKER_SHIPMENT_REGISTERED, ExchangeRound.FIRST_EXCHANGE));

        Notification notification = captureOne();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getReceiver().getId()).isEqualTo(2L);
        assertThat(notification.getTitle()).isEqualTo("책이 오고 있어요!");
        assertThat(notification.getMessage()).isEqualTo("보낸이님이 실제 책을 발송했어요. 운송장 번호를 확인해보세요.");
        assertThat(notification.getDedupKey())
                .isEqualTo("delivery:tracking-registered:group:10:round:FIRST_EXCHANGE:delivery:delivery-1");
        assertThat(payload.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "redirectType", "groupId", "exchangeType", "exchangeRound", "deliveryId"
        );
        assertThat(payload.get("redirectType").asText()).isEqualTo("TRACKER_DETAIL");
        assertThat(payload.get("exchangeType").asText()).isEqualTo("DELIVERY");
    }

    @Test
    void firstAndReturnRoundsUseDifferentDedupKeys() {
        service.send(event(NotificationType.TRACKER_SHIPMENT_REGISTERED, ExchangeRound.FIRST_EXCHANGE));
        service.send(event(NotificationType.TRACKER_SHIPMENT_REGISTERED, ExchangeRound.RETURN_EXCHANGE));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getDedupKey).containsExactly(
                "delivery:tracking-registered:group:10:round:FIRST_EXCHANGE:delivery:delivery-1",
                "delivery:tracking-registered:group:10:round:RETURN_EXCHANGE:delivery:delivery-1"
        );
    }

    @Test
    void retryingSameShipmentRegistrationUsesSameDedupKey() {
        DeliveryNotificationEvent event = event(
                NotificationType.TRACKER_SHIPMENT_REGISTERED,
                ExchangeRound.FIRST_EXCHANGE
        );

        service.send(event);
        service.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Notification::getDedupKey).containsOnly(
                "delivery:tracking-registered:group:10:round:FIRST_EXCHANGE:delivery:delivery-1"
        );
    }

    @Test
    void storesReceiveConfirmationForDeliverySender() {
        service.send(event(NotificationType.TRACKER_DELIVERY_CONFIRMED, ExchangeRound.FIRST_EXCHANGE));

        Notification notification = captureOne();
        assertThat(notification.getReceiver().getId()).isEqualTo(2L);
        assertThat(notification.getTitle()).isEqualTo("내가 보낸 책이 안전하게 도착했어요");
        assertThat(notification.getMessage()).isEqualTo("보낸이님이 실제 책을 잘 받았다고 해요!");
        assertThat(notification.getDedupKey())
                .isEqualTo("delivery:received-confirmed:group:10:round:FIRST_EXCHANGE:delivery:delivery-1");
    }

    @Test
    void skipsNotificationWhenReceiverIsActor() {
        DeliveryNotificationEvent event = new DeliveryNotificationEvent(
                NotificationType.TRACKER_SHIPMENT_REGISTERED,
                1L,
                "본인",
                1L,
                10L,
                ExchangeRound.FIRST_EXCHANGE,
                "delivery-1",
                "책"
        );

        service.send(event);

        verify(notificationStore, never()).save(any());
    }

    @Test
    void logsDuplicateDedupCollisionAtDebugLevelWithoutFailing() {
        doReturn(Optional.empty()).when(notificationStore).save(any());
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        service.send(event(NotificationType.TRACKER_SHIPMENT_REGISTERED, ExchangeRound.FIRST_EXCHANGE));

        assertThat(appender.list).anySatisfy(log -> {
            assertThat(log.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(log.getFormattedMessage()).contains("Duplicate delivery notification ignored");
        });
    }

    @Test
    void logsUnexpectedDatabaseFailureAtErrorLevelWithoutPropagating() {
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(notificationStore).save(any());
        ListAppender<ILoggingEvent> appender = attachLogAppender();

        service.send(event(NotificationType.TRACKER_SHIPMENT_REGISTERED, ExchangeRound.FIRST_EXCHANGE));

        assertThat(appender.list).anySatisfy(log -> {
            assertThat(log.getLevel()).isEqualTo(Level.ERROR);
            assertThat(log.getFormattedMessage()).contains("Delivery notification database operation failed");
            assertThat(log.getThrowableProxy()).isNotNull();
        });
    }

    private DeliveryNotificationEvent event(NotificationType type, ExchangeRound round) {
        return new DeliveryNotificationEvent(
                type,
                1L,
                "보낸이",
                2L,
                10L,
                round,
                "delivery-1",
                "실제 책"
        );
    }

    private Notification captureOne() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        return captor.getValue();
    }

    private ListAppender<ILoggingEvent> attachLogAppender() {
        serviceLogger = (Logger) LoggerFactory.getLogger(DeliveryNotificationService.class);
        originalLogLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(Level.DEBUG);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
        return logAppender;
    }
}
