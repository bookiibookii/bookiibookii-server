package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import org.springframework.stereotype.Component;

@Component
public class TrackerConverter {

    public TrackerDetailResponse toDetailResponse(Tracker tracker){
        return TrackerDetailResponse.builder()
                .trackerId(tracker.getId())
                .trackerStatus(tracker.getTrackerStatus())
                .currentMatchedMemberId(tracker.getCurrentMember() != null ?
                        tracker.getCurrentMember().getMatchedMember() : null)
                .endDate(tracker.getEndDate())
                .extension_count(tracker.getExtensionCount())
                .extension_days(tracker.getExtensionDays())
                .build();
    }


    public TrackerHistoryResponse toHistoryResponse(TrackerHistory history, Long senderUserId, Long receiverUserId) {
        return TrackerHistoryResponse.builder()
                .trackerId(history.getTracker().getId())
                .groupId(history.getTracker().getGroupId())
                // Service에서 조회해서 넘겨준 유저의 진짜 ID
                .senderUserId(senderUserId)
                .receiverUserId(receiverUserId)
                .trackerStatus(history.getTrackerStatus())
                .deliveryCompany(history.getDeliveryCompany())
                .trackingNumber(history.getTrackingNumber())
                .start_date(history.getStartDate())
                .end_date(history.getEndDate())
                .createdAt(history.getCreatedAt())
                .build();
    }

    public TrackerListResponse toListResponse(Tracker tracker, String role) {
        return TrackerListResponse.builder()
                .groupId(tracker.getGroupId())
                .bookTitle("제목 없음")
                .status(tracker.getTrackerStatus())
                .role(role)
                .build();
    }

}
