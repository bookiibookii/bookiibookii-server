package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CommentNotificationServiceTest {

    @Mock private NotificationStore notificationStore;
    @Mock private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CommentNotificationService service;

    @BeforeEach
    void setUp() {
        when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        service = new CommentNotificationService(
                notificationStore,
                new NotificationFactory(userRepository, objectMapper)
        );
    }

    @Test
    void storesNewCommentNotificationWithCommentPayload() throws Exception {
        service.send(new CommentEvent(
                NotificationType.GROUP_COMMENT_CREATED,
                "작성자",
                "교환 독서",
                List.of(20L),
                1L,
                100L,
                null
        ));

        Notification notification = captureNotification();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getType()).isEqualTo(NotificationType.GROUP_COMMENT_CREATED);
        assertThat(notification.getReceiver().getId()).isEqualTo(20L);
        assertThat(notification.getTitle()).isEqualTo("새로운 댓글이 달렸어요");
        assertThat(notification.getMessage()).isEqualTo("작성자님이 교환 독서 그룹에 새 댓글을 남겼어요.");
        assertThat(payload.get("redirectType").asText()).isEqualTo("GROUP_DETAIL");
        assertThat(payload.get("groupId").asLong()).isEqualTo(1L);
        assertThat(payload.get("commentId").asLong()).isEqualTo(100L);
        assertThat(payload.has("parentCommentId")).isFalse();
        assertThat(payload.has("requestId")).isFalse();
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-GRP-004:100:20");
    }

    @Test
    void storesOnlyReplyTypeWithParentPayload() throws Exception {
        service.send(new CommentEvent(
                NotificationType.GROUP_COMMENT_REPLIED,
                "답글 작성자",
                "교환 독서",
                List.of(20L),
                1L,
                101L,
                100L
        ));

        Notification notification = captureNotification();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getType()).isEqualTo(NotificationType.GROUP_COMMENT_REPLIED);
        assertThat(notification.getReceiver().getId()).isEqualTo(20L);
        assertThat(notification.getTitle()).isEqualTo("내 댓글에 답글이 달렸어요");
        assertThat(notification.getMessage()).isEqualTo("답글 작성자님이 회원님의 댓글에 답글을 남겼어요.");
        assertThat(payload.get("redirectType").asText()).isEqualTo("GROUP_DETAIL");
        assertThat(payload.get("groupId").asLong()).isEqualTo(1L);
        assertThat(payload.get("commentId").asLong()).isEqualTo(101L);
        assertThat(payload.get("parentCommentId").asLong()).isEqualTo(100L);
        assertThat(payload.has("requestId")).isFalse();
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-GRP-005:101:20");
    }

    @Test
    void createsReceiverSpecificDedupKeysForMultipleRecipients() {
        service.send(new CommentEvent(
                NotificationType.GROUP_COMMENT_CREATED,
                "호스트",
                "교환 독서",
                List.of(20L, 30L),
                1L,
                100L,
                null
        ));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .containsExactly("NOTI-GRP-004:100:20", "NOTI-GRP-004:100:30");
    }

    private Notification captureNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        return captor.getValue();
    }
}
