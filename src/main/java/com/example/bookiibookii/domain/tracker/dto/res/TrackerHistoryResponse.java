package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class TrackerHistoryResponse {
    private Long trackerId;
    private Long groupId;
    private Long senderUserId;
    private Long receiverUserId;
    private TrackerStatus trackerStatus;  // 당시 상태 (SHIPPING, RECEIVED 등)
    private String DeliveryCompany;
    private String trackingNumber; // 운송장 번호
    private LocalDateTime start_date;
    private LocalDateTime end_date;
    private LocalDateTime createdAt; // 기록 일시
}