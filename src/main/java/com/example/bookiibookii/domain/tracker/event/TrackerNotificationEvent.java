package com.example.bookiibookii.domain.tracker.event;

import com.example.bookiibookii.domain.tracker.enums.TrackerNotiType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;

import java.time.LocalDateTime;

public record TrackerNotificationEvent(
        TrackerNotiType notiType,

        // 발신자
        Long actorId,

        // redirect
        Long groupId,
        ExchangeRound exchangeRound,

        // 알림 내용
        LocalDateTime returnDueAt // 반납 예정일
) {}
