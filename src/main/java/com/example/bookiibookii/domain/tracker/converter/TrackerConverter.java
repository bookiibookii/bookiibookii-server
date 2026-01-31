package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
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
                .groupId(history.getTracker().getGroup().getGroupId())
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

    public TrackerListResponse toListResponse(Tracker tracker, String targetNickname, List<String> stepDates) {
        // Groups 엔티티에서 필요한 정보 추출
        Groups group = tracker.getGroup();

        return TrackerListResponse.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getGroupComment() != null ? group.getGroupComment() : "제목 없음")
                .author("저자 미상") // 추후 도서 엔티티 연동 시 수정
                .targetNickname(targetNickname) // 서비스에서 조회한 상대방 닉네임
                .stepDates(stepDates) // [읽는 중, 배송 중, 게스트 읽는 중, 회수 중] 순서의 날짜 리스트
                .build();
    }
}
