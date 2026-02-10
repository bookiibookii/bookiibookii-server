package com.example.bookiibookii.domain.notification.converter;

import com.example.bookiibookii.domain.notification.dto.KeywordResDTO;
import com.example.bookiibookii.domain.notification.dto.NotificationResDTO;
import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.entity.UserKeyword;
import com.example.bookiibookii.domain.notification.enums.KeywordSort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NotificationConverter {
    private final ObjectMapper objectMapper;

    /**
     * 키워드 목록 DTO 변환
     */
    public KeywordResDTO.KeywordList toKeywordListRes(List<UserKeyword> links, KeywordSort sort) {
        List<KeywordResDTO.KeywordItem> items = links.stream()
                .map(uk -> toKeywordItemRes(uk.getKeyword()))
                .toList();

        return KeywordResDTO.KeywordList.builder()
                .keywordSort(sort)
                .keywordNumber(items.size())
                .keywordList(items)
                .build();
    }

    public KeywordResDTO.KeywordItem toKeywordItemRes(Keyword keyword) {
        return KeywordResDTO.KeywordItem.builder()
                .keywordId(keyword.getId())
                .content(keyword.getContent())
                .build();
    }

    /**
     * 알림 목록 응답 변환
     */
    public NotificationResDTO.NotificationListRes toNotificationListRes(
            List<Notification> page, String nextCursor, boolean hasNext) {

        List<NotificationResDTO.NotificationItemRes> items = page.stream()
                .map(this::toNotificationItemRes)
                .toList();

        return new NotificationResDTO.NotificationListRes(items, nextCursor, hasNext);
    }

    public NotificationResDTO.NotificationItemRes toNotificationItemRes(Notification n) {
        return new NotificationResDTO.NotificationItemRes(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt(),
                parsePayload(n.getPayload())
        );
    }

    public NotificationResDTO.NotificationReadRes toNotificationReadRes(Notification n) {
        return new NotificationResDTO.NotificationReadRes(
                n.getId(),
                n.getType().name(),
                n.isRead(),
                n.getReadAt(),
                parsePayload(n.getPayload())
        );
    }

    private Map<String, Object> parsePayload(String payload) {
        if (payload == null || payload.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
