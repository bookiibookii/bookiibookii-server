package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
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

    public TrackerDetailResponse toDetailResponse(Tracker tracker) {
        return TrackerDetailResponse.builder()
                .trackerId(tracker.getId())
                .trackerStatus(tracker.getTrackerStatus())
                .currentMatchedMemberId(tracker.getBookOwner() != null ?
                        tracker.getBookOwner().getId() : null)
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
        Groups group = tracker.getGroup();

        String groupType = group.getGroupType().toString();

        Book book = group.getBook();

        // 3. 최상위 공통 빌더 구성
        TrackerListResponse.TrackerListResponseBuilder builder = TrackerListResponse.builder()
                .groupId(group.getGroupId())
                .groupType(groupType)
                .bookTitle(book.getTitle())
                .image(book.getImage())
                .author(book.getAuthor())
                .category(book.getCategory().toString());

        // 4. 타입별 상세 데이터 매핑
        if ("RELAY".equals(groupType)) {
            builder.relayDetail(TrackerListResponse.RelayDetail.builder()
                    .hostNickname(group.getHost().getName())
                     .hostProfileImage(group.getHost().getUserImage() != null ? group.getHost().getUserImage().getS3Key() : null)
                    .stepDates(stepDates)
                    .build());
        }
        else if ("TOGETHER".equals(groupType)) {
            builder.togetherDetail(TrackerListResponse.TogetherDetail.builder()
                    .hostNickname(group.getHost().getName())
                    .participantCount(group.getMatchedMember().size())
                    .myReadingRate(0) // 추후 계산 로직 연결
                    .groupReadingRate(0) // 추후 계산 로직 연결
                    .build());
        }

        return builder.build();
    }
}