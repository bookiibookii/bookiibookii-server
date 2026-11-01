package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.event.ReadingCardReactionNotificationEvent;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingCardReactionNotificationServiceTest {

    @Mock
    private NotificationStore notificationStore;
    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReadingCardReactionNotificationService service;

    @BeforeEach
    void setUp() {
        when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        service = new ReadingCardReactionNotificationService(
                notificationStore,
                new NotificationFactory(userRepository, objectMapper)
        );
    }

    @Test
    void storesReactionNotificationOncePerCardAndReactorKey() throws Exception {
        ReadingCardReactionNotificationEvent event =
                new ReadingCardReactionNotificationEvent(2L, "파트너", 1L, 10L, 15L, 20L);

        service.send(event);
        service.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        org.mockito.Mockito.verify(notificationStore, org.mockito.Mockito.times(2)).save(captor.capture());
        Notification notification = captor.getAllValues().get(0);
        JsonNode payload = objectMapper.readTree(notification.getPayload());

        assertThat(notification.getTitle()).isEqualTo("파트너가 반응을 남겼어요");
        assertThat(notification.getMessage())
                .isEqualTo("파트너님이 회원님의 독서카드에 반응을 남겼어요.");
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .containsOnly("READING_CARD_REACTION_CREATED:card:20:reactor:2");
        assertThat(notification.getType()).isEqualTo(NotificationType.READING_CARD_REACTION_CREATED);
        assertThat(payload.has("notificationCode")).isFalse();
        assertThat(payload.has("type")).isFalse();
        assertThat(payload.get("redirectType").asText()).isEqualTo("BOOK_CARD_DETAIL");
        assertThat(payload.get("groupId").asLong()).isEqualTo(10L);
        assertThat(payload.get("memberBookId").asLong()).isEqualTo(15L);
        assertThat(payload.get("cardId").asLong()).isEqualTo(20L);
    }
}
