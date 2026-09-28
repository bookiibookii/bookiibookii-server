package com.example.bookiibookii.domain.push.listener;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.event.NotificationPushRequestedEvent;
import com.example.bookiibookii.domain.push.service.PushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationPushEventListenerTest {

    @Mock
    private PushService pushService;

    @Test
    void buildsPushDataFromWhitelistedNotificationPayload() {
        NotificationPushEventListener listener =
                new NotificationPushEventListener(pushService, new ObjectMapper());
        NotificationPushRequestedEvent event = new NotificationPushRequestedEvent(
                100L,
                20L,
                "GROUP_COMMENT_CREATED",
                "title",
                "body",
                """
                {
                  "redirectType": "GROUP_DETAIL",
                  "groupId": 30,
                  "notificationCode": "must-not-leak",
                  "cardId": 35,
                  "memberBookId": 36,
                  "commentId": 40,
                  "keyword": "must-not-leak",
                  "actorId": 50
                }
                """
        );

        listener.handle(event);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushService).sendToUser(org.mockito.ArgumentMatchers.eq(20L), captor.capture());
        assertThat(captor.getValue().title()).isEqualTo("title");
        assertThat(captor.getValue().body()).isEqualTo("body");
        assertThat(captor.getValue().data())
                .containsEntry("notificationId", "100")
                .containsEntry("type", "GROUP_COMMENT_CREATED")
                .containsEntry("redirectType", "GROUP_DETAIL")
                .containsEntry("groupId", "30")
                .containsEntry("cardId", "35")
                .containsEntry("memberBookId", "36")
                .containsEntry("commentId", "40")
                .doesNotContainKeys("notificationType", "notificationCode", "keyword", "actorId");
    }
}
