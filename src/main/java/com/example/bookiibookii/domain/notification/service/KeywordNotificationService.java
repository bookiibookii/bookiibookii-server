package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.repository.UserKeywordRepository;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordNotificationService {

    private final UserKeywordRepository userKeywordRepository;
    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(KeywordGroupCreatedEvent event) {
        if (event.keywordIds() == null || event.keywordIds().isEmpty()) return;

        List<Long> receiverIds =
                userKeywordRepository.findDistinctUserIdsByKeywordIds(event.keywordIds());
        if (receiverIds.isEmpty()) return;

        String payload = notificationFactory.toJson(
                NotificationPayload.builder()
                        .redirectType(RedirectType.GROUP_DETAIL)
                        .groupId(event.groupId())
                        .build()
        );
        String title = "찾으시는 책이 올라왔어요!";
        String keywordPart = formatKeywords(event.keywordTexts());
        String message = String.format("%s 관련 새 그룹이 생성되었습니다. 마감되기 전에 신청해보세요.", keywordPart);

        receiverIds.forEach(id -> notificationStore.save(
                notificationFactory.create(
                        id,
                        NotificationCategory.KEYWORD,
                        NotificationType.KEYWORD_GROUP_CREATED,
                        title,
                        message,
                        payload
                )
        ));
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
