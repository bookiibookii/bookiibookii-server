package com.example.bookiibookii.domain.tracker.listener;

import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TrackerEventListener {

    private final TrackerService trackerService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupMatched(GroupMatchedEvent event) {
        try {
            trackerService.createTracker(event);
        } catch (Exception e) {
            throw new TrackerException(TrackerErrorCode.TRACKER_NOT_CREATED);
        }
    }
}