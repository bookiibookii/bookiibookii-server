package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
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
                .bookTitle(book != null ? book.getTitle() : null)
                .image(book != null ? book.getImage() : null)
                .author(book != null ? book.getAuthor() : null)
                .category(book != null && book.getCategory() != null ? book.getCategory().toString() : null);

        // 4. 타입별 상세 데이터 매핑
        if (group.getGroupType() == GroupType.RELAY) {
            // 게스트 프로필 이미지 리스트 추출 (호스트 제외)
            List<String> guestImages = group.getMatchedMember().stream()
                    .map(matched -> matched.getUser()) // MatchedMember에서 User 추출
                    .filter(user -> !user.getRole().equals(RoleStatus.HOST)) // 호스트는 제외
                    .map(user -> user.getUserImage() != null ? user.getUserImage().getS3Key() : null) // 이미지 경로 추출
                    .toList();

            builder.relayDetail(TrackerListResponse.RelayDetail.builder()
                    .partnerNickname(targetNickname) // 서비스에서 조회한 현재 나의 파트너 닉네임
                    .hostProfileImage(group.getHost().getUserImage() != null ? group.getHost().getUserImage().getS3Key() : null)
                    .guestProfileImages(guestImages) // 위에서 추출한 게스트 이미지 리스트
                    .stepDates(stepDates)
                    .build());
        }
        else if (group.getGroupType() == GroupType.TOGETHER) {
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