package com.example.bookiibookii.domain.tracker.event;

import com.example.bookiibookii.domain.tracker.enums.TrackerAction;

import java.time.LocalDateTime;

public record TrackerNotificationEvent(
        TrackerAction action,

        // 발신자
        Long actorId,

        // redirect
        Long groupId,

        // 알림 내용
        LocalDateTime returnDueAt // 반납 예정일
) {}
