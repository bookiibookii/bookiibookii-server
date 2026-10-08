package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.entity.UserKeyword;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.repository.UserKeywordRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordNotificationServiceTest {

    @Mock private UserKeywordRepository userKeywordRepository;
    @Mock private NotificationStore notificationStore;
    @Mock private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KeywordNotificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        service = new KeywordNotificationService(
                userKeywordRepository,
                notificationStore,
                new NotificationFactory(userRepository, objectMapper)
        );
    }

    @Test
    void storesKeywordNotificationWithKeywordPayloadAndDedupKey() throws Exception {
        Keyword keyword = keyword(10L, "경제");
        User receiver = User.builder().id(20L).build();
        when(userKeywordRepository.findAllWithUserAndKeywordByKeywordIds(List.of(keyword.getId())))
                .thenReturn(List.of(subscription(receiver, keyword)));

        service.send(new KeywordGroupCreatedEvent(
                1L, 30L, List.of(keyword.getId())));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        Notification notification = captor.getValue();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(payload.get("groupId").asLong()).isEqualTo(1L);
        assertThat(payload.get("keyword").asText()).isEqualTo("경제");
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-KWD-001:1:10");
    }

    @Test
    void doesNotNotifyGroupHostForOwnKeyword() {
        Keyword keyword = keyword(10L, "경제");
        User host = User.builder().id(30L).build();
        when(userKeywordRepository.findAllWithUserAndKeywordByKeywordIds(List.of(keyword.getId())))
                .thenReturn(List.of(subscription(host, keyword)));

        service.send(new KeywordGroupCreatedEvent(
                1L, host.getId(), List.of(keyword.getId())));

        verify(notificationStore, never()).save(any());
    }

    private Keyword keyword(Long id, String content) {
        return Keyword.builder()
                .id(id)
                .content(content)
                .normalizedContent(content)
                .prefix2(content.substring(0, Math.min(2, content.length())))
                .build();
    }

    private UserKeyword subscription(User user, Keyword keyword) {
        return UserKeyword.builder().user(user).keyword(keyword).build();
    }
}
