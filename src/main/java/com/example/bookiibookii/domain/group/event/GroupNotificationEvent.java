package com.example.bookiibookii.domain.group.event;

import com.example.bookiibookii.domain.group.enums.GroupNotiType;

public record GroupNotificationEvent(
        GroupNotiType type,

        // 발신자
        Long actorId,

        // 특정 수신자 - (수신자가 여러명일시 null 처리)
        Long receiverId,

        // redirect
        Long groupId
) {}
