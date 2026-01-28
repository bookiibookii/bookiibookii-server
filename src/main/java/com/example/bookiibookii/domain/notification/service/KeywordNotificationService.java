package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.event.KeywordGroupMatchedEvent;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.notification.repository.UserKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordNotificationService {

    private final UserKeywordRepository userKeywordRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationFactory notificationFactory;

    @Transactional
    public void send(KeywordGroupMatchedEvent event) {
        List<Long> receiverIds =
                userKeywordRepository.findDistinctUserIdsByKeywordIds(event.keywordIds());
        if (receiverIds.isEmpty()) return;

        String payload = notificationFactory.toJson(Map.of("groupId", event.groupId()));
        String title = "찾으시는 책이 올라왔어요!";
        String keywordPart = formatKeywords(event.keywordTexts());
        String message = String.format("%s 관련 새 그룹이 생성되었습니다. 마감되기 전에 신청해보세요.", keywordPart);

        notificationRepository.saveAll(
                receiverIds.stream()
                        .map(id -> notificationFactory.create(id, NotificationType.KEYWORD, title, message, payload))
                        .toList()
        );
    }

    // keyword 알림 메시지 format
    private String formatKeywords(List<String> keywordTexts) {
        if (keywordTexts == null || keywordTexts.isEmpty()) {
            return "키워드";
        }
        String joined = keywordTexts.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        if (joined.isBlank()) return "키워드";
        return "키워드 " + joined;
    }
}
