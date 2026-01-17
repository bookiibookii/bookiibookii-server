package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.tracker.dto.response.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
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

}
