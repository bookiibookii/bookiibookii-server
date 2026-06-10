package com.example.bookiibookii.domain.notification.dto;

import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationPayload(
        RedirectType redirectType,
        Long groupId,
        ExchangeType exchangeType,
        ExchangeRound exchangeRound,
        Long commentId,
        Long parentCommentId,
        String deliveryId,
        Long meetingId,
        Long bookCardId,
        Long noticeId,
        Long keywordId,
        String keyword,
        Long requestId,
        Long reviewId,
        Long actorId,
        Long matchedMemberId,
        Long bookId
) {
}
