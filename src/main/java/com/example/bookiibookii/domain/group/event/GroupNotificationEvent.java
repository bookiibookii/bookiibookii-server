package com.example.bookiibookii.domain.group.event;

import com.example.bookiibookii.domain.group.enums.GroupNotiType;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;

import java.util.List;

public record GroupNotificationEvent(
        GroupNotiType type,

        // 발신자
        Long actorId,

        String groupTitle,

        // 특정 수신자
        Long receiverId,
        // 다수 수신자
        List<Long> receiverIds,

        // redirect
        Long groupId,
        Long requestId,
        ExchangeType exchangeType
) {}
