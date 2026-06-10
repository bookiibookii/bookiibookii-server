package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {

    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(CommentEvent event) {
        Long receiverId = event.hostId();
        String title = "새로운 댓글이 달렸어요";
        String message = String.format("%s님이 %s 그룹에 댓글을 남겼어요. 확인해볼까요?",
                event.commenterNickname(), event.bookTitle());
        String payload = notificationFactory.toJson(
                NotificationPayload.builder()
                        .redirectType(RedirectType.GROUP_DETAIL)
                        .groupId(event.groupId())
                        .build()
        );

        notificationStore.save(
                notificationFactory.create(receiverId, NotificationCategory.SYSTEM, NotificationType.GROUP_COMMENT_CREATED,
                        title, message, payload)
        );
    }
}
