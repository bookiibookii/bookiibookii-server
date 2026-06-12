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
        DisplayBooks displayBooks = resolveDisplayBooks(me, partner);
        DisplayMetadata displayMetadata = resolveDisplayMetadata(displayBooks.myBook(), displayStatus);

        return TrackerListItemResDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .tradeType(group.getTradeType())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .displayBookTitle(displayMetadata.bookTitle())
                .displayStatusLabel(displayMetadata.statusLabel())
                .remainingDays(remainingDays)
                .myCurrentBook(toBookInfo(
                        displayBooks.myBook(),
                        me.getUser(),
                        myCurrentReaderProfileImageUrl,
                        isMyOriginalBook(displayBooks.myBook(), me),
                        displayBooks.startsNewReading()
                ))
                .partnerCurrentBook(toBookInfo(
                        displayBooks.partnerBook(),
                        partner.getUser(),
                        partnerCurrentReaderProfileImageUrl,
                        isMyOriginalBook(displayBooks.partnerBook(), me),
                        displayBooks.startsNewReading()
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
        DisplayBooks displayBooks = resolveDisplayBooks(me, partner);
        DisplayMetadata displayMetadata = resolveDisplayMetadata(displayBooks.myBook(), displayStatus);

        return TrackerDetailResDTO.builder()
                .groupId(group.getId())
                .groupName(group.getGroupName())
                .tradeType(group.getTradeType())
                .myRole(me.getRole())
                .displayStatus(displayStatus)
                .displayBookTitle(displayMetadata.bookTitle())
                .displayStatusLabel(displayMetadata.statusLabel())
                .dDay(dDay)
                .myBook(toBookInfo(
                        displayBooks.myBook(),
                        me.getUser(),
                        myProfileImageUrl,
                        isMyOriginalBook(displayBooks.myBook(), me),
                        displayBooks.startsNewReading()
                ))
                .partnerBook(toBookInfo(
                        displayBooks.partnerBook(),
                        partner.getUser(),
                        partnerProfileImageUrl,
                        isMyOriginalBook(displayBooks.partnerBook(), me),
                        displayBooks.startsNewReading()
                ))
                .steps(steps)
                .build();
    }

    public static BookInfo toBookInfo(
            MemberBook memberBook,
            String currentReaderProfileImageUrl,
            boolean isMyOriginalBook
    ) {
        return toBookInfo(
                memberBook,
                memberBook.getMatchedMember().getUser(),
                currentReaderProfileImageUrl,
                isMyOriginalBook,
                false
        );
    }

    private static BookInfo toBookInfo(
            MemberBook memberBook,
            User currentReader,
            String currentReaderProfileImageUrl,
            boolean isMyOriginalBook,
            boolean startsNewReading
    ) {
        Book book = memberBook.getBook();
        int currentPage = startsNewReading ? 0 : memberBook.getCurrentPage();

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

    private static DisplayBooks resolveDisplayBooks(MatchedMember me, MatchedMember partner) {
        if (shouldSwapBooksForPackageTrackerDisplay(me, partner)) {
            return new DisplayBooks(
                    partner.getCurrentMemberBook(),
                    me.getCurrentMemberBook(),
                    me.getReadingStatus() == ReadingStatus.EXCHANGING
            );
        }

        return new DisplayBooks(
                me.getCurrentMemberBook(),
                partner.getCurrentMemberBook(),
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

    private static DisplayMetadata resolveDisplayMetadata(
            MemberBook displayBook,
            TrackerDisplayStatus displayStatus
    ) {
        return new DisplayMetadata(
                displayBook.getBook().getTitle(),
                resolveDisplayStatusLabel(displayStatus)
        );
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
            boolean startsNewReading
    ) {
    }

    private record DisplayMetadata(
            String bookTitle,
            String statusLabel
    ) {
    }
}
