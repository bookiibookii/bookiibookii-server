package com.example.bookiibookii.domain.tracker.listener;

import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackerEventListener {

    private final TrackerService trackerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupMatched(GroupMatchedEvent event) {
        log.info("[TrackerEventListener] 매칭 완료 이벤트 수신 성공 - groupId: {}", event.groupId());
        try {
            trackerService.createTracker(event);
        } catch (Exception e) {
            log.error("[TrackerEventListener] 트래커 생성 중 예외 발생: ", e);
        }
    }
}