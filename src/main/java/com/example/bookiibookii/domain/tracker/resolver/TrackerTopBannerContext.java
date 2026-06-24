package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;

import java.time.LocalDate;
import java.time.Instant;

public record TrackerTopBannerContext(
        Long groupId,
        Long matchedMemberId,
        String groupName,
        String partnerNickname,
        TradeType tradeType,
        RoleStatus myRole,
        ReadingStatus readingStatus,
        ExchangeStatus myExchangeStatus,
        ExchangeStatus partnerExchangeStatus,
        LocalDate readingEndDate,
        String currentReadingBookTitle,
        ExchangeRound exchangeRound,
        String firstExchangeSendBookTitle,
        String returnExchangeSendBookTitle,
        boolean partnerReviewWritten,
        Instant meetingAt,
        String meetingPlaceName
) {
}
