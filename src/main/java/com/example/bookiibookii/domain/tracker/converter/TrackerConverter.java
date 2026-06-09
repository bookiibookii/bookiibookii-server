package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
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
        DisplayBooks displayBooks = resolveDisplayBooks(
                me,
                partner,
                myCurrentReaderProfileImageUrl,
                partnerCurrentReaderProfileImageUrl
        );

        return TrackerListItemResDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .tradeType(group.getTradeType())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .remainingDays(remainingDays)
                .myCurrentBook(toBookInfo(
                        displayBooks.myBook(),
                        displayBooks.myProfileImageUrl(),
                        isMyOriginalBook(displayBooks.myBook(), me),
                        displayBooks.resetProgressForDisplay()
                ))
                .partnerCurrentBook(toBookInfo(
                        displayBooks.partnerBook(),
                        displayBooks.partnerProfileImageUrl(),
                        isMyOriginalBook(displayBooks.partnerBook(), me),
                        displayBooks.resetProgressForDisplay()
                ))
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
        DisplayBooks displayBooks = resolveDisplayBooks(
                me,
                partner,
                myProfileImageUrl,
                partnerProfileImageUrl
        );

        return TrackerDetailResDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .tradeType(group.getTradeType())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .displayBookTitle(resolveDisplayBookTitle(displayBooks.myBook()))
                .displayStatusLabel(resolveDisplayStatusLabel(displayStatus))
                .dDay(dDay)
                .myBook(toBookInfo(
                        displayBooks.myBook(),
                        displayBooks.myProfileImageUrl(),
                        isMyOriginalBook(displayBooks.myBook(), me),
                        displayBooks.resetProgressForDisplay()
                ))
                .partnerBook(toBookInfo(
                        displayBooks.partnerBook(),
                        displayBooks.partnerProfileImageUrl(),
                        isMyOriginalBook(displayBooks.partnerBook(), me),
                        displayBooks.resetProgressForDisplay()
                ))
                .steps(steps)
                .build();
    }

    public static BookInfo toBookInfo(
            MemberBook memberBook,
            String currentReaderProfileImageUrl,
            boolean isMyOriginalBook
    ) {
        return toBookInfo(memberBook, currentReaderProfileImageUrl, isMyOriginalBook, false);
    }

    private static BookInfo toBookInfo(
            MemberBook memberBook,
            String currentReaderProfileImageUrl,
            boolean isMyOriginalBook,
            boolean resetProgressForDisplay
    ) {
        Book book = memberBook.getBook();
        User currentReader = memberBook.getMatchedMember().getUser();
        int currentPage = resetProgressForDisplay ? 0 : memberBook.getCurrentPage();

        return BookInfo.builder()
                .title(book.getTitle())
                .image(book.getImage())
                .totalPages(book.getTotalPages())
                .currentPage(currentPage)
                .isMyOriginalBook(isMyOriginalBook)
                .currentReaderNickname(currentReader.getNickName())
                .currentReaderProfileImageUrl(currentReaderProfileImageUrl)
                .currentReadingRate(calculateProgressRate(currentPage, book.getTotalPages()))
                .build();
    }

    private static boolean isMyOriginalBook(MemberBook memberBook, MatchedMember me) {
        return memberBook.isMine() == memberBook.isOwnedBy(me);
    }

    private static int calculateProgressRate(Integer currentPage, Integer totalPages) {
        if (currentPage == null || totalPages == null || totalPages <= 0) {
            return 0;
        }
        int normalizedPage = Math.min(Math.max(currentPage, 0), totalPages);
        return (normalizedPage * 100) / totalPages;
    }

    private static DisplayBooks resolveDisplayBooks(
            MatchedMember me,
            MatchedMember partner,
            String myProfileImageUrl,
            String partnerProfileImageUrl
    ) {
        if (shouldSwapBooksForPackageTrackerDisplay(me, partner)) {
            return new DisplayBooks(
                    partner.getCurrentMemberBook(),
                    me.getCurrentMemberBook(),
                    partnerProfileImageUrl,
                    myProfileImageUrl,
                    true
            );
        }

        return new DisplayBooks(
                me.getCurrentMemberBook(),
                partner.getCurrentMemberBook(),
                myProfileImageUrl,
                partnerProfileImageUrl,
                false
        );
    }

    private static boolean shouldSwapBooksForPackageTrackerDisplay(MatchedMember me, MatchedMember partner) {
        return me.getGroup().getTradeType() == TradeType.DELIVERY
                && isPackageExchangeBooksInTransit(me.getReadingStatus())
                && hasBothMembersRegisteredTracking(me, partner);
    }

    private static boolean isPackageExchangeBooksInTransit(ReadingStatus readingStatus) {
        return readingStatus == ReadingStatus.EXCHANGING || readingStatus == ReadingStatus.RETURNING;
    }

    private static boolean hasBothMembersRegisteredTracking(MatchedMember me, MatchedMember partner) {
        return isTrackingRegisteredOrReceived(me.getExchangeStatus())
                && isTrackingRegisteredOrReceived(partner.getExchangeStatus());
    }

    private static boolean isTrackingRegisteredOrReceived(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.TRACKING_REGISTERED
                || exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED;
    }

    private static String resolveDisplayBookTitle(MemberBook displayBook) {
        return displayBook.getBook().getTitle();
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

    private record DisplayBooks(
            MemberBook myBook,
            MemberBook partnerBook,
            String myProfileImageUrl,
            String partnerProfileImageUrl,
            boolean resetProgressForDisplay
    ) {
    }
}
