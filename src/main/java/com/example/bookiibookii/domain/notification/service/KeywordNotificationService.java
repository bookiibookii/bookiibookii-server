package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.entity.UserKeyword;
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

@Service
@RequiredArgsConstructor
public class KeywordNotificationService {

    private final UserKeywordRepository userKeywordRepository;
    private final NotificationStore notificationStore;
    private final NotificationFactory notificationFactory;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(KeywordGroupCreatedEvent event) {
        if (event.keywordIds() == null || event.keywordIds().isEmpty()) return;

        List<UserKeyword> subscriptions =
                userKeywordRepository.findAllWithUserAndKeywordByKeywordIds(event.keywordIds());

        for (UserKeyword subscription : subscriptions) {
            Long receiverId = subscription.getUser().getId();
            if (receiverId.equals(event.hostId())) continue;

            String keyword = subscription.getKeyword().getContent();
            String payload = notificationFactory.toJson(
                    NotificationPayload.builder()
                            .redirectType(RedirectType.GROUP_DETAIL)
                            .groupId(event.groupId())
                            .keywordId(subscription.getKeyword().getId())
                            .keyword(keyword)
                            .build()
            );
            String title = String.format("%s 관련 그룹을 확인해보세요", keyword);
            String message = String.format(
                    "키워드 %s 관련 새 그룹이 생성됐어요. 마감되기 전에 확인해보세요.",
                    keyword
            );
            String dedupKey = String.format(
                    "NOTI-KWD-001:%d:%d",
                    event.groupId(),
                    subscription.getKeyword().getId()
            );

            notificationStore.save(
                    notificationFactory.create(
                        receiverId,
                        NotificationCategory.KEYWORD,
                        NotificationType.KEYWORD_GROUP_CREATED,
                        title,
                        message,
                        payload,
                        dedupKey
                )
            );
        }
    }
}
