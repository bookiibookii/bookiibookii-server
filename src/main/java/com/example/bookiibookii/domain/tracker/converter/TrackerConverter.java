package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponseDTO;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.user.entity.Address;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackerConverter {

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final UserImageS3Service userImageS3Service;


    public TrackerDetailResponseDTO toDetailResponse(Tracker tracker, Meeting latestMeeting,
                                                     Address partnerAddress, User partnerUser,
                                                     TrackerHistory latestHistory) {

        int remainingDays = calculateRemainingDays(tracker, latestMeeting);

        // 1. 공통 빌더 생성
        TrackerDetailResponseDTO.TrackerDetailResponseDTOBuilder builder = TrackerDetailResponseDTO.builder()
                .trackerId(tracker.getId())
                .bookTitle(tracker.getGroup().getBook().getTitle())
                .partnerNickname(partnerUser.getNickName())
                .trackerStatus(tracker.getTrackerStatus())
                .startDate(tracker.getStartDate())
                .endDate(tracker.getEndDate())
                .extensionCount(tracker.getExtensionCount())
                .extensionDays(tracker.getExtensionDays())
                .readingPeriod(tracker.getGroup().getReadingPeriod())
                .remainingDays(remainingDays);

        // 2. 직접 교환(DIRECT)에 따른 정보 세팅
        // v1에서는 DELIVERY 타입을 지원하지 않으므로 DIRECT 관련 로직만 유지합니다.
        if (tracker.getGroup().getTradeType() == TradeType.DIRECT) {
            if (latestMeeting != null) {
                // 약속이 잡힌 경우: 약속 시간과 장소 노출
                builder.meetingInfo(TrackerDetailResponseDTO.MeetingInfo.builder()
                        .meetingTime(latestMeeting.getMeetingTime())
                        .meetingPlace(latestMeeting.getMeetingPlace())
                        .build());
            } else {
                // 약속 전인 경우: 호스트가 설정한 선호 지역(기본 장소) 노출
                builder.meetingInfo(TrackerDetailResponseDTO.MeetingInfo.builder()
                        .meetingPlace(tracker.getGroup().getPreferRegion())
                        .build());
            }
        }

        return builder.build();
    }


    public TrackerHistoryResponseDTO toHistoryResponse(TrackerHistory history, Long senderUserId, Long receiverUserId) {
        return TrackerHistoryResponseDTO.builder()
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

    public TrackerListResponseDTO toListResponse(Tracker tracker, Groups group,
                                                 String targetNickname, List<String> stepDates,
                                                 Integer myRate, Integer groupRate) {

        String groupType = group.getGroupType().toString();

        Book book = group.getBook();

        // 3. 최상위 공통 빌더 구성
        TrackerListResponseDTO.TrackerListResponseDTOBuilder builder = TrackerListResponseDTO.builder()
                .groupId(group.getGroupId())
                .groupType(groupType)
                .bookTitle(book != null ? book.getTitle() : null)
                .bookImage(book != null ? book.getImage() : null)
                .bookAuthor(book != null ? book.getAuthor() : null)
                .bookCategory(book != null && book.getCategory() != null ? book.getCategory().toString() : null)
                .tradeType(group.getTradeType());

        // 4. 타입별 상세 데이터 매핑
        if (group.getGroupType() == GroupType.RELAY) {
            // 게스트 프로필 이미지 Presigned GET URL 리스트 추출 (호스트 제외)
            List<String> guestProfileImageUrls = group.getMatchedMember().stream()
                    .filter(mm -> !mm.getRole().equals(RoleStatus.HOST)) // 그룹 내 역할 기준으로 호스트 제외
                    .map(MatchedMember::getUser) // MatchedMember에서 User 추출
                    .map(user -> {
                        if (user.getUserImage() == null) {
                            return null;
                        }
                        return userImageS3Service.generatePresignedGetUrl(
                                user.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
                    })
                    .toList();

            String hostProfileImageUrl = null;
            if (group.getHost().getUserImage() != null) {
                hostProfileImageUrl = userImageS3Service.generatePresignedGetUrl(
                        group.getHost().getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
            }

            builder.relayDetail(TrackerListResponseDTO.RelayDetail.builder()
                    .partnerNickname(targetNickname) // 서비스에서 조회한 현재 나의 파트너 닉네임
                    .hostProfileImageUrl(hostProfileImageUrl)
                    .guestProfileImageUrls(guestProfileImageUrls) // 위에서 추출한 게스트 이미지 Presigned GET URL 리스트
                    .stepDates(stepDates)
                    .build());
        }
        return builder.build();
    }

    public int calculateRemainingDays(Tracker tracker, Meeting latestMeeting) {
        //  '현재 날짜'를 기준으로 비교
        LocalDate today = LocalDate.now();
        TrackerStatus status = tracker.getTrackerStatus();

        // 1. 독서 중인 경우 (현재 날짜부터 종료 예정일까지 남은 일수)
        if (status == TrackerStatus.HOST_READING || status == TrackerStatus.GUEST_READING ||
        status == TrackerStatus.HOST_EXTENSION || status == TrackerStatus.GUEST_EXTENSION) {
            if (tracker.getEndDate() == null) return 0;
            return (int) ChronoUnit.DAYS.between(today, tracker.getEndDate().toLocalDate());
        }

        // 2. 직접 교환 약속이 있는 경우 (오늘부터 약속 날짜까지)
        if ((status == TrackerStatus.SHIPPING_TO_GUEST || status == TrackerStatus.SHIPPING_TO_HOST)
                && latestMeeting != null && latestMeeting.getMeetingTime() != null) {
            return (int) ChronoUnit.DAYS.between(today, latestMeeting.getMeetingTime().toLocalDate());
        }

        // 3. 3일 제한이 있는 상태들
        List<TrackerStatus> threeDayLimitStatuses = List.of(
                TrackerStatus.HOST_DONE, TrackerStatus.RECEIVED,
                TrackerStatus.GUEST_DONE, TrackerStatus.RETURNED
        );

        if (threeDayLimitStatuses.contains(status)) {
            LocalDateTime baseTime = tracker.getUpdatedAt() != null ? tracker.getUpdatedAt() : LocalDateTime.now();
            LocalDate deadline = baseTime.plusDays(3).toLocalDate();
            return (int) ChronoUnit.DAYS.between(today, deadline);
        }

        return 0;
    }
}