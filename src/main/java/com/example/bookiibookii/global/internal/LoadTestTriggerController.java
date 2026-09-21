package com.example.bookiibookii.global.internal;

import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.event.KeywordGroupCreatedEvent;
import com.example.bookiibookii.domain.notification.repository.KeywordRepository;
import com.example.bookiibookii.domain.notification.service.KeywordNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@ConditionalOnProperty(name = "internal.test.enabled", havingValue = "true")
@RequestMapping("/internal/test/trigger")
@RequiredArgsConstructor
public class LoadTestTriggerController {

    private final KeywordNotificationService keywordNotificationService;
    private final KeywordRepository keywordRepository;

    private final AtomicLong fakeGroupIdSeq = new AtomicLong(-1L);

    @PostMapping("/keyword-notification")
    public ResponseEntity<Void> triggerKeywordNotification() {
        Keyword keyword = keywordRepository.findByNormalizedContent("test_kn_keyword")
                .orElseThrow(() -> new IllegalStateException(
                        "TEST_KN_ keyword not found. Run seed-keyword-notification.sql first."));

        keywordNotificationService.send(new KeywordGroupCreatedEvent(
                fakeGroupIdSeq.getAndDecrement(),
                0L,
                List.of(keyword.getId())
        ));

        return ResponseEntity.ok().build();
    }
}
