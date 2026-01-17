package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.tracker.dto.response.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.response.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import org.springframework.stereotype.Component;

import java.util.List;

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
                .DeliveryCompany(history.getDeliveryCompany())
                .trackingNumber(history.getTrackingNumber())
                .start_date(history.getStartDate())
                .end_date(history.getEndDate())
                .createdAt(history.getCreatedAt())
                .build();
    }

}
