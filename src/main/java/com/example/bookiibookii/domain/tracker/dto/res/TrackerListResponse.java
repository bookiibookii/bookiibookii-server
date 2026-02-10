package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrackerListResponse {
    // 공통 필드
    private Long groupId;
    private String groupType; // RELAY, TOGETHER
    private String bookTitle;
    private String bookImage;
    private String bookAuthor;
    private String bookCategory;

    // 거래방식
    private TradeType tradeType;

    // 타입별 상세 데이터
    private RelayDetail relayDetail;
    private TogetherDetail togetherDetail;

    @Getter
    @Builder
    public static class RelayDetail {
        private String partnerNickname;         // 파트너 닉네임
        private String hostProfileImageUrl;        // 호스트 프로필 이미지 Presigned GET URL
        private List<String> guestProfileImageUrls; // 게스트들의 프로필 이미지 Presigned GET URL 리스트
        private TrackerStatus trackerStatus;           // 트래커 상태
        private List<String> stepDates; // [4] 단계별 날짜 (예: ["12.01", null, null, null])
    }

    @Getter
    @Builder
    public static class TogetherDetail {
        private String hostNickname;
        private int participantCount; // 참여 인원 수
        private int myReadingRate;    // 나의 독서율 (0~100)
        private int groupReadingRate; // 그룹 전체 평균 독서율
    }
}