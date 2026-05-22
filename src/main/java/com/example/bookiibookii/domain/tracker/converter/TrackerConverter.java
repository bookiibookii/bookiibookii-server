package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListItemResDTO;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TrackerConverter {

    // 트래커 리스트 조회
    public static TrackerListItemResDTO toListItem(
            MatchedMember me,
            MatchedMember partner,
            TrackerDisplayStatus displayStatus,
            Integer remainingDays
    ) {
        Groups group = me.getGroup();

        // 실제 수령 전이어도 운송장 등록 시점부터 상대 책을 display 대상으로 전환한다.
        boolean displayPartnerBookFromTrackingRegistration = me.getReadingStatus() == ReadingStatus.EXCHANGING
                && (me.getExchangeStatus() == ExchangeStatus.TRACKING_REGISTERED
                || me.getExchangeStatus() == ExchangeStatus.RECEIVED_CONFIRMED);
        MemberBook myDisplayBook = displayPartnerBookFromTrackingRegistration
                ? partner.getCurrentMemberBook()
                : me.getCurrentMemberBook();
        MemberBook partnerDisplayBook = displayPartnerBookFromTrackingRegistration
                ? me.getCurrentMemberBook()
                : partner.getCurrentMemberBook();

        return TrackerListItemResDTO.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .displayStatus(displayStatus)
                .tradeType(group.getTradeType())
                .remainingDays(remainingDays)
                .myCurrentBook(toBookInfo(myDisplayBook))
                .partnerCurrentBook(toBookInfo(partnerDisplayBook))
                .build();
    }

    // 트래커(교환독서) 상세조회
    public static TrackerDetailResDTO toDetail(
            MatchedMember me,
            MatchedMember partner,
            TrackerDisplayStatus displayStatus,
            Integer dDay,
            List<TrackerStepInfo> steps
    ) {
        Groups group = me.getGroup();

        return TrackerDetailResDTO.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .displayStatusText(resolveDisplayStatusText(me, displayStatus))
                .dDay(dDay)
                .myBook(toBookInfo(me.getCurrentMemberBook()))
                .partnerBook(toBookInfo(partner.getCurrentMemberBook()))
                .steps(steps)
                .build();
    }

    public static BookInfo toBookInfo(MemberBook memberBook) {
        Book book = memberBook.getBook();
        User currentReader = memberBook.getMatchedMember().getUser();

        return BookInfo.builder()
                .title(book.getTitle())
                .image(book.getImage())
                .totalPages(book.getTotalPages())
                .currentPage(memberBook.getCurrentPage())
                .isOwnerBook(memberBook.isMine())
                .currentReaderNickname(currentReader.getNickName())
                .currentReadingRate(calculateProgressRate(memberBook.getCurrentPage(), book.getTotalPages()))
                .build();
    }

    private static int calculateProgressRate(Integer currentPage, Integer totalPages) {
        if (currentPage == null || totalPages == null || totalPages <= 0) {
            return 0;
        }
        int normalizedPage = Math.min(Math.max(currentPage, 0), totalPages);
        return (normalizedPage * 100) / totalPages;
    }

    private static String resolveDisplayStatusText(
            MatchedMember me,
            TrackerDisplayStatus displayStatus
    ) {
        String bookTitle = me.getCurrentMemberBook().getBook().getTitle();

        return bookTitle + " · " + resolveDisplayStatusLabel(displayStatus);
    }

    private static String resolveDisplayStatusLabel(TrackerDisplayStatus displayStatus) {
        return switch (displayStatus) {
            case READING -> "읽는 중";
            case REVIEW_WRITING -> "후기 작성";

            case TRACKING_REQUIRED -> "운송장 등록";
            case SHIPPING -> "배송 중";
            case RETURN_TRACKING_REQUIRED -> "반납 운송장 등록";
            case RETURNING -> "반납 중";

            case MEETING_REQUIRED -> "약속 등록";
            case EXCHANGING -> "교환 중";

            case EXCHANGE_REVIEW_WRITING -> "교환 후기 작성";
        };
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
            if (latestMeeting != null && latestMeeting.getScheduledAt() != null) {
                Location loc = latestMeeting.getLocation();
                builder.meetingInfo(TrackerDetailResponseDTO.MeetingInfo.builder()
                        .meetingTime(latestMeeting.getScheduledAt())
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
                && latestMeeting != null && latestMeeting.getScheduledAt() != null) {
            return (int) Math.max(0, ChronoUnit.DAYS.between(today, latestMeeting.getScheduledAt().toLocalDate()));
        }

        return 0;
    }
}
