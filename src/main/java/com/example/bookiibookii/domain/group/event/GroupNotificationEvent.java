package com.example.bookiibookii.domain.group.event;

import com.example.bookiibookii.domain.group.enums.GroupNotiType;

import java.util.List;

public record GroupNotificationEvent(
        GroupNotiType type,

        // 발신자
        Long actorId,

        String bookTitle,

        // 특정 수신자
        Long receiverId,
        // 다수 수신자
        List<Long> receiverIds,

        // redirect
        Long groupId
) {}
