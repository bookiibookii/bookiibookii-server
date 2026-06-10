package com.example.bookiibookii.domain.comment.event;

import com.example.bookiibookii.domain.notification.enums.NotificationType;

import java.util.List;

public record CommentEvent(
        NotificationType notificationType,
        String commenterNickname,
        String groupTitle,
        List<Long> receiverIds,
        Long groupId,
        Long commentId,
        Long parentCommentId
) {}
