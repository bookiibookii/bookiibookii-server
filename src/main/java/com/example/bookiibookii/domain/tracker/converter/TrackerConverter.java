package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListItemResDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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

        // 실제 수령 전이어도 운송장 등록 시점부터 상대 책을 display 대상으로 전환한다.
        boolean displayPartnerBookFromTrackingRegistration = me.getReadingStatus() == ReadingStatus.EXCHANGING
                && (me.getExchangeStatus() == ExchangeStatus.TRACKING_REGISTERED
                || me.getExchangeStatus() == ExchangeStatus.RECEIVED_CONFIRMED);
        MemberBook myDisplayBook = displayPartnerBookFromTrackingRegistration ? partner.getCurrentMemberBook() : me.getCurrentMemberBook();
        MemberBook partnerDisplayBook = displayPartnerBookFromTrackingRegistration ? me.getCurrentMemberBook() : partner.getCurrentMemberBook();
        String myDisplayProfileImageUrl = displayPartnerBookFromTrackingRegistration
                ? partnerCurrentReaderProfileImageUrl
                : myCurrentReaderProfileImageUrl;
        String partnerDisplayProfileImageUrl = displayPartnerBookFromTrackingRegistration
                ? myCurrentReaderProfileImageUrl
                : partnerCurrentReaderProfileImageUrl;

        return TrackerListItemResDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .tradeType(group.getTradeType())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .remainingDays(remainingDays)
                .myCurrentBook(toBookInfo(myDisplayBook, myDisplayProfileImageUrl))
                .partnerCurrentBook(toBookInfo(partnerDisplayBook, partnerDisplayProfileImageUrl))
                .build();
    }

    // 트래커(교환독서) 상세조회
    public static TrackerDetailResDTO toDetail(
            MatchedMember me,
            MatchedMember partner,
            TrackerDisplayStatus displayStatus,
            Integer dDay,
            String myProfileImageUrl,
            String partnerProfileImageUrl,
            List<TrackerStepInfo> steps
    ) {
        Groups group = me.getGroup();

        return TrackerDetailResDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .tradeType(group.getTradeType())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .displayBookTitle(resolveDisplayBookTitle(me))
                .displayStatusLabel(resolveDisplayStatusLabel(displayStatus))
                .dDay(dDay)
                .myBook(toBookInfo(
                        me.getCurrentMemberBook(),
                        myProfileImageUrl
                ))
                .partnerBook(toBookInfo(
                        partner.getCurrentMemberBook(),
                        partnerProfileImageUrl
                ))
                .steps(steps)
                .build();
    }

    public static BookInfo toBookInfo(MemberBook memberBook, String currentReaderProfileImageUrl) {
        Book book = memberBook.getBook();
        User currentReader = memberBook.getMatchedMember().getUser();

        return BookInfo.builder()
                .title(book.getTitle())
                .image(book.getImage())
                .totalPages(book.getTotalPages())
                .currentPage(memberBook.getCurrentPage())
                .isOwnerBook(memberBook.isMine())
                .currentReaderNickname(currentReader.getNickName())
                .currentReaderProfileImageUrl(currentReaderProfileImageUrl)
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

    private static String resolveDisplayBookTitle(MatchedMember me) {
        return me.getCurrentMemberBook().getBook().getTitle();
    }

    private static String resolveDisplayStatusLabel(TrackerDisplayStatus displayStatus) {
        return switch (displayStatus) {
            case READING -> "읽는 중";
            case REVIEW_WRITING -> "후기 작성";
            case REVIEW_WAITING_PARTNER -> "후기 수정";

            case TRACKING_REQUIRED -> "운송장 등록";
            case SHIPPING -> "배송 중";
            case RETURN_TRACKING_REQUIRED -> "반납 운송장 등록";
            case RETURNING -> "반납 중";
            case WAITING_PARTNER_TRACKING_REGISTER -> "상대 운송장 등록 대기";
            case WAITING_PARTNER_RECEIPT_CONFIRM -> "상대 수령 인증 대기";

            case MEETING_REQUIRED -> "약속 등록";
            case MEETING_REGISTER_REQUIRED -> "약속 등록";
            case WAITING_HOST_MEETING_REGISTER -> "약속 등록 대기";
            case WAITING_PARTNER_MEETING_COMPLETE -> "상대 교환 완료 대기";
            case EXCHANGING -> "교환 중";

            case EXCHANGE_REVIEW_WRITING -> "교환 후기 작성";
        };
    }
}
