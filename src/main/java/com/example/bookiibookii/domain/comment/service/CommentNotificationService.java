package com.example.bookiibookii.domain.comment.service;

import com.example.bookiibookii.domain.comment.event.CommentEvent;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.notification.service.NotificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(CommentEvent event) {
        Long receiverId = event.hostId();
        String title = "새로운 댓글이 달렸어요";
        String message = String.format("%s님이 %s 그룹에 댓글을 남겼어요. 확인해볼까요?",
                event.commenterNickname(), event.bookTitle());
        String payload = notificationFactory.toJson(Map.of("groupId", event.groupId()));

        notificationRepository.save(
                notificationFactory.create(receiverId, NotificationType.SYSTEM, title, message, payload)
        );
    }
}
