package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.Location;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponseDTO;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackerConverter {

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final UserImageS3Service userImageS3Service;

    public TrackerDetailResponseDTO toDetailResponse(Tracker tracker, Meeting latestMeeting,
                                                     Delivery latestShippingDelivery, User partnerUser) {

        int remainingDays = calculateRemainingDays(tracker, latestMeeting);

        TrackerDetailResponseDTO.TrackerDetailResponseDTOBuilder builder = TrackerDetailResponseDTO.builder()
                .trackerId(tracker.getId())
                .bookTitle(tracker.getGroup().getBook().getTitle())
                .partnerNickname(partnerUser.getNickName())
                .trackerStatus(tracker.getTrackerStatus())
                .startDate(tracker.getStartDate())
                .endDate(tracker.getEndDate())
                .startedAt(tracker.getStartedAt())
                .completedAt(tracker.getCompletedAt())
                .extensionCount(tracker.getExtensionCount())
                .extensionDays(tracker.getExtensionDays())
                .readingPeriod(tracker.getGroup().getReadingPeriod())
                .remainingDays(remainingDays);

        if (latestShippingDelivery != null) {
            builder.deliveryInfo(TrackerDetailResponseDTO.DeliveryInfo.builder()
                    .deliveryCompany(latestShippingDelivery.getDeliveryCompany())
                    .trackingNumber(latestShippingDelivery.getTrackingNumber())
                    .build());
        }

        if (tracker.getGroup().getTradeType() == TradeType.DIRECT) {
            if (latestMeeting != null && latestMeeting.getMeetingTime() != null) {
                Location loc = latestMeeting.getLocation();
                builder.meetingInfo(TrackerDetailResponseDTO.MeetingInfo.builder()
                        .meetingTime(latestMeeting.getMeetingTime())
                        .placeName(loc != null ? loc.getPlaceName() : null)
                        .address(loc != null ? loc.getAddress() : null)
                        .build());
            } else {
                builder.meetingInfo(TrackerDetailResponseDTO.MeetingInfo.builder()
                        .placeName(tracker.getGroup().getPreferRegion())
                        .build());
            }
        }

        return builder.build();
    }

    public TrackerListResponseDTO toListResponse(Tracker tracker, Groups group,
                                                 String targetNickname, List<String> stepDates,
                                                 Integer myRate, Integer groupRate) {

        String groupType = group.getGroupType().toString();
        Book book = group.getBook();

        TrackerListResponseDTO.TrackerListResponseDTOBuilder builder = TrackerListResponseDTO.builder()
                .groupId(group.getGroupId())
                .groupType(groupType)
                .bookTitle(book != null ? book.getTitle() : null)
                .bookImage(book != null ? book.getImage() : null)
                .bookAuthor(book != null ? book.getAuthor() : null)
                .bookCategory(book != null && book.getCategory() != null ? book.getCategory().toString() : null)
                .tradeType(group.getTradeType());

        if (group.getGroupType() == GroupType.RELAY) {
            List<String> guestProfileImageUrls = group.getMatchedMember().stream()
                    .filter(mm -> !mm.getRole().equals(RoleStatus.HOST))
                    .map(MatchedMember::getUser)
                    .map(user -> {
                        if (user.getUserImage() == null) return null;
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
                    .partnerNickname(targetNickname)
                    .hostProfileImageUrl(hostProfileImageUrl)
                    .guestProfileImageUrls(guestProfileImageUrls)
                    .trackerStatus(tracker.getTrackerStatus())
                    .stepDates(stepDates)
                    .build());
        }

        return builder.build();
    }

    public int calculateRemainingDays(Tracker tracker, Meeting latestMeeting) {
        LocalDate today = LocalDate.now();
        TrackerStatus status = tracker.getTrackerStatus();

        if (status == TrackerStatus.MY_BOOK_READING || status == TrackerStatus.PARTNER_BOOK_READING) {
            if (tracker.getEndDate() == null) return 0;
            return (int) Math.max(0, ChronoUnit.DAYS.between(today, tracker.getEndDate().toLocalDate()));
        }

        if ((status == TrackerStatus.EXCHANGING || status == TrackerStatus.RETURNING)
                && latestMeeting != null && latestMeeting.getMeetingTime() != null) {
            return (int) Math.max(0, ChronoUnit.DAYS.between(today, latestMeeting.getMeetingTime().toLocalDate()));
        }

        return 0;
    }
}
