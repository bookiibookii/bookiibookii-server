package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListItemResDTO;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class TrackerConverter {

    // 트래커 리스트 조회
    public static TrackerListItemResDTO toListItem(
            MatchedMember me,
            MatchedMember partner,
            TrackerDisplayStatus displayStatus,
            Integer remainingDays,
            String myCurrentReaderProfileImageUrl,
            String partnerCurrentReaderProfileImageUrl
    ) {
        Groups group = me.getGroup();

        return TrackerListItemResDTO.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .displayStatus(displayStatus)
                .tradeType(group.getTradeType())
                .remainingDays(remainingDays)
                .myCurrentBook(toBookInfo(me.getCurrentMemberBook(), myCurrentReaderProfileImageUrl))
                .partnerCurrentBook(toBookInfo(partner.getCurrentMemberBook(), partnerCurrentReaderProfileImageUrl))
                .build();
    }

    public static BookInfo toBookInfo(MemberBook memberBook, String currentReaderProfileImageUrl) {
        Book book = memberBook.getBook();
        User currentReader = memberBook.getMatchedMember().getUser();

        return BookInfo.builder()
                .title(book.getTitle())
                .image(book.getImage())
                .isOwnerBook(memberBook.isMine())
                .currentReaderNickname(currentReader.getNickName())
                .currentReaderProfileImageUrl(currentReaderProfileImageUrl)
                .currentReadingRate(toReadingRate(memberBook.getProgressRate()))
                .build();
    }

    private static int toReadingRate(Double progressRate) {
        if (progressRate == null) {
            return 0;
        }
        return (int) Math.round(progressRate);
    }

    public TrackerDetailResponseDTO toDetailResponse(Tracker tracker, Meeting latestMeeting,
                                                     Delivery latestShippingDelivery, User partnerUser) {

        int remainingDays = calculateRemainingDays(tracker, latestMeeting);

        TrackerDetailResponseDTO.TrackerDetailResponseDTOBuilder builder = TrackerDetailResponseDTO.builder()
                .trackerId(tracker.getId())
                .bookTitle(tracker.getGroup().getBook().getTitle())
                .partnerNickname(partnerUser.getNickName())
                .readingStatus(tracker.getReadingStatus())
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

    public int calculateRemainingDays(Tracker tracker, Meeting latestMeeting) {
        LocalDate today = LocalDate.now();
        ReadingStatus status = tracker.getReadingStatus();

        if (status == ReadingStatus.MY_BOOK_READING || status == ReadingStatus.PARTNER_BOOK_READING) {
            if (tracker.getEndDate() == null) return 0;
            return (int) Math.max(0, ChronoUnit.DAYS.between(today, tracker.getEndDate().toLocalDate()));
        }

        if ((status == ReadingStatus.EXCHANGING || status == ReadingStatus.RETURNING)
                && latestMeeting != null && latestMeeting.getMeetingTime() != null) {
            return (int) Math.max(0, ChronoUnit.DAYS.between(today, latestMeeting.getMeetingTime().toLocalDate()));
        }

        return 0;
    }
}
