package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.entity.UserKeyword;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
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
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeywordNotificationServiceTest {

    @Mock private UserKeywordRepository userKeywordRepository;
    @Mock private NotificationStore notificationStore;
    @Mock private UserRepository userRepository;
    @Captor private ArgumentCaptor<List<Notification>> notificationsCaptor;

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
        Keyword otherKeyword = keyword(11L, "소설");
        User receiver = User.builder().id(20L).build();
        User otherReceiver = User.builder().id(21L).build();
        User host = User.builder().id(30L).build();
        List<Long> keywordIds = List.of(keyword.getId(), otherKeyword.getId());
        when(userKeywordRepository.findAllWithUserAndKeywordByKeywordIds(keywordIds))
                .thenReturn(List.of(
                        subscription(receiver, keyword),
                        subscription(host, keyword),
                        subscription(otherReceiver, keyword),
                        subscription(receiver, otherKeyword)));

        service.send(new KeywordGroupCreatedEvent(
                1L, host.getId(), keywordIds));

        verify(notificationStore).saveAll(notificationsCaptor.capture());
        verifyNoMoreInteractions(notificationStore);
        List<Notification> notifications = notificationsCaptor.getValue();
        assertThat(notifications).hasSize(3);
        assertKeywordNotification(notifications.get(0), receiver, keyword);
        assertKeywordNotification(notifications.get(1), otherReceiver, keyword);
        assertKeywordNotification(notifications.get(2), receiver, otherKeyword);
    }

    private void assertKeywordNotification(Notification notification, User receiver, Keyword keyword)
            throws Exception {
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getReceiver().getId()).isEqualTo(receiver.getId());
        assertThat(notification.getCategory()).isEqualTo(NotificationCategory.KEYWORD);
        assertThat(notification.getType()).isEqualTo(NotificationType.KEYWORD_GROUP_CREATED);
        assertThat(notification.getTitle()).isEqualTo(keyword.getContent() + " 관련 그룹을 확인해보세요");
        assertThat(notification.getMessage()).isEqualTo(
                "키워드 " + keyword.getContent() + " 관련 새 그룹이 생성됐어요. 마감되기 전에 확인해보세요.");
        assertThat(notification.isRead()).isFalse();
        assertThat(payload.get("redirectType").asText()).isEqualTo("GROUP_DETAIL");
        assertThat(payload.get("groupId").asLong()).isEqualTo(1L);
        assertThat(payload.get("keywordId").asLong()).isEqualTo(keyword.getId());
        assertThat(payload.get("keyword").asText()).isEqualTo(keyword.getContent());
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-KWD-001:1:" + keyword.getId());
    }

    @Test
    void doesNotNotifyGroupHostForOwnKeyword() {
        Keyword keyword = keyword(10L, "경제");
        User host = User.builder().id(30L).build();
        when(userKeywordRepository.findAllWithUserAndKeywordByKeywordIds(List.of(keyword.getId())))
                .thenReturn(List.of(subscription(host, keyword)));

        service.send(new KeywordGroupCreatedEvent(
                1L, host.getId(), List.of(keyword.getId())));

        verify(notificationStore).saveAll(List.of());
        verifyNoMoreInteractions(notificationStore);
    }

    @Test
    void doesNotQueryOrStoreNotificationsWithoutKeywords() {
        service.send(new KeywordGroupCreatedEvent(1L, 30L, List.of()));

        verifyNoInteractions(userKeywordRepository, notificationStore, userRepository);
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
