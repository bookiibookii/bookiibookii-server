package com.example.bookiibookii.domain.tracker.listener;

import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TrackerEventListener {

    private final TrackerService trackerService;

    // 그룹 스케줄러의 트랜잭션이 성공적으로 'Commit' 된 후에 실행되도록 설정
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupMatched(GroupMatchedEvent event) {
        trackerService.createTracker(event);
    }
}