package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.user.entity.Address;
import com.example.bookiibookii.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackerConverter {


    public TrackerDetailResponse toDetailResponse(Tracker tracker, Meeting latestMeeting,
                                                  Address partnerAddress, User partnerUser,
                                                  TrackerHistory latestHistory) {
        // 1. 공통 빌더 생성
        TrackerDetailResponse.TrackerDetailResponseBuilder builder = TrackerDetailResponse.builder()
                .trackerId(tracker.getId())
                .bookTitle(tracker.getGroup().getBook().getTitle())
                .partnerNickname(partnerUser.getNickName())
                .trackerStatus(tracker.getTrackerStatus())
                .startDate(tracker.getStartDate())
                .endDate(tracker.getEndDate())
                .extensionCount(tracker.getExtensionCount())
                .extensionDays(tracker.getExtensionDays())
                .readingPeriod(tracker.getGroup().getReadingPeriod());

        // 2. TradeType에 따른 분기 처리
        TradeType tradeType = tracker.getGroup().getTradeType();

        if (tradeType == TradeType.DELIVERY) {
            if (partnerAddress != null) {
                // DeliveryInfo 빌더 생성
                TrackerDetailResponse.DeliveryInfo.DeliveryInfoBuilder deliveryBuilder = TrackerDetailResponse.DeliveryInfo.builder()
                        .receiverName(partnerAddress.getReceiverName())
                        .receiverPhone(partnerAddress.getPhone())
                        .receiverAddress(String.format("[%s] %s %s",
                                partnerAddress.getZipCode(),
                                partnerAddress.getAddress(),
                                partnerAddress.getAddressDetail()))
                        .isVerified(tracker.getIsVerified());

                // 운송 정보 추출
                if (latestHistory != null) {
                    deliveryBuilder.deliveryCompany(latestHistory.getDeliveryCompany())
                            .trackingNumber(latestHistory.getTrackingNumber());
                }

                builder.deliveryInfo(deliveryBuilder.build());
            }
        } else if (tradeType == TradeType.DIRECT) {
            // 직접 교환일 경우: 최신 약속 정보 세팅
            if (latestMeeting != null) {
                builder.meetingInfo(TrackerDetailResponse.MeetingInfo.builder()
                        .meetingTime(latestMeeting.getMeetingTime())
                        .meetingPlace(latestMeeting.getMeetingPlace())
                        .build());
            } else {
                // 약속 전이면 호스트가 설정한 기본 장소 노출
                builder.meetingInfo(TrackerDetailResponse.MeetingInfo.builder()
                        .meetingPlace(tracker.getGroup().getHost().getMeetPlace())
                        .build());
            }
        }

        return builder.build();
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

    public TrackerListResponse toListResponse(Tracker tracker, Groups group,
                                              String targetNickname, List<String> stepDates,
                                              Integer myRate, Integer groupRate) {

        String groupType = group.getGroupType().toString();

        Book book = group.getBook();

        // 3. 최상위 공통 빌더 구성
        TrackerListResponse.TrackerListResponseBuilder builder = TrackerListResponse.builder()
                .groupId(group.getGroupId())
                .groupType(groupType)
                .bookTitle(book != null ? book.getTitle() : null)
                .bookImage(book != null ? book.getImage() : null)
                .bookAuthor(book != null ? book.getAuthor() : null)
                .bookCategory(book != null && book.getCategory() != null ? book.getCategory().toString() : null)
                .tradeType(group.getTradeType());

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
                    .hostNickname(group.getHost().getNickName())
                    .participantCount(group.getMatchedMember().size())
                    .myReadingRate(myRate)
                    .groupReadingRate(groupRate)
                    .build());
        }

        return builder.build();
    }
}