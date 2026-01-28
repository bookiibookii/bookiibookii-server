package com.example.bookiibookii.domain.comment.event;

public record CommentEvent(
        // 알림 내용
        String commenterNickname,
        String bookTitle,

        // 수신자
        Long hostId,

        // redirect
        Long groupId
) {}
