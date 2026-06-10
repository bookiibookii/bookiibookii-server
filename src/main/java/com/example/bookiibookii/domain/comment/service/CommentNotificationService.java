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
        if (event.receiverIds() == null || event.receiverIds().isEmpty()) return;

        boolean reply = event.notificationType() == NotificationType.GROUP_COMMENT_REPLIED;
        String title = reply ? "내 댓글에 답글이 달렸어요" : "새로운 댓글이 달렸어요";
        String message = reply
                ? String.format("%s님이 회원님의 댓글에 답글을 남겼어요.", event.commenterNickname())
                : String.format("%s님이 %s 그룹에 새 댓글을 남겼어요.",
                        event.commenterNickname(), event.groupTitle());
        String payload = notificationFactory.toJson(
                NotificationPayload.builder()
                        .redirectType(RedirectType.GROUP_DETAIL)
                        .groupId(event.groupId())
                        .commentId(event.commentId())
                        .parentCommentId(event.parentCommentId())
                        .build()
        );

        for (Long receiverId : event.receiverIds()) {
            if (receiverId == null) continue;
            String notiId = reply ? "NOTI-GRP-005" : "NOTI-GRP-004";
            String dedupKey = String.format("%s:%d:%d", notiId, event.commentId(), receiverId);
            notificationStore.save(
                    notificationFactory.create(
                            receiverId,
                            NotificationCategory.SYSTEM,
                            event.notificationType(),
                            title,
                            message,
                            payload,
                            dedupKey
                    )
            );
        }
    }
}
