package com.example.bookiibookii.domain.notification.event;

import java.util.List;

public record KeywordGroupMatchedEvent(
        Long groupId,
        List<String> keywordTexts,
        List<Long> keywordIds
) {}
