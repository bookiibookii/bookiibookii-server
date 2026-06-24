package com.example.bookiibookii.domain.notification.event;

public record ReadingCardReactionNotificationEvent(
        Long reactorId,
        String reactorNickname,
        Long receiverId,
        Long groupId,
        Long cardId
) {
}
