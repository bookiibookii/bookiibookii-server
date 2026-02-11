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
    private String bookTitle;           // 책 제목
    private String partnerNickname;     // 파트너 닉네임
    private TrackerStatus trackerStatus; // 도서 상태
    private LocalDateTime startDate;     // 대여 시작일
    private LocalDateTime endDate;       // 반납 예정일
    private Integer extensionCount;      // 연장 횟수
    private Integer extensionDays;       // 연장 일수
    private Integer readingPeriod;      // 그룹 독서 기간
    private Long trackerId;
    private Integer remainingDays;

    private DeliveryInfo deliveryInfo;

    private MeetingInfo meetingInfo;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DeliveryInfo {
        // 상대방 배송지 정보
        private String receiverName;    // 받는 분 성함
        private String receiverPhone;   // 연락처
        private String receiverAddress; // 주소

        // 배송 정보
        private String deliveryCompany;     // 택배사명
        private String trackingNumber;  // 운송장 번호

        // 상대방 수령 확인의 확인 검증용
        private Boolean isVerified;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MeetingInfo {
        private LocalDateTime meetingTime; // 약속 날짜 및 시간
        private String meetingPlace;       // 약속 장소
    }
}
