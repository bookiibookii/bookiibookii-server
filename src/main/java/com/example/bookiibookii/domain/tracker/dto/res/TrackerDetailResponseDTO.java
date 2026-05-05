package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TrackerDetailResponseDTO {
    private String bookTitle;
    private String partnerNickname;
    private TrackerStatus trackerStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer extensionCount;
    private Integer extensionDays;
    private Integer readingPeriod;
    private Long trackerId;
    private Integer remainingDays;

    private DeliveryInfo deliveryInfo;
    private MeetingInfo meetingInfo;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DeliveryInfo {
        private String deliveryCompany;
        private String trackingNumber;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MeetingInfo {
        private LocalDateTime meetingTime;
        private String placeName;
        private String address;
    }
}
