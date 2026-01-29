package com.example.bookiibookii.domain.notification.event;

import java.util.List;

public record KeywordGroupCreatedEvent(
        Long groupId,
        List<String> keywordTexts,
        List<Long> keywordIds
) {}
