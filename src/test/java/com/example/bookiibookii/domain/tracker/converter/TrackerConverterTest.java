package com.example.bookiibookii.domain.tracker.converter;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListItemResDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerConverterTest {

    @Test
    void detailProgressUsesMemberBookCurrentPageInsteadOfReadingCardPage() {
        Groups group = Groups.builder()
                .id(1L)
                .groupName("교환 독서")
                .tradeType(TradeType.DIRECT)
                .build();
        MatchedMember me = member(1L, 10L, "me", group, RoleStatus.GUEST, ExchangeStatus.NOT_STARTED);
        MatchedMember partner = member(2L, 20L, "partner", group, RoleStatus.HOST, ExchangeStatus.NOT_STARTED);
        MemberBook myBook = memberBook(1L, me, "내 책", true);
        MemberBook partnerBook = memberBook(2L, partner, "상대 책", true);
        me.changeCurrentMemberBook(myBook, java.time.Instant.now());
        partner.changeCurrentMemberBook(partnerBook, java.time.Instant.now());
        myBook.updateCurrentPage(30);
        myBook.getCards().add(Cards.builder()
                .id(1L)
                .memberBook(myBook)
                .cardType(CardType.TEXT)
                .page(100)
                .quotation("카드 기록")
                .build());

        TrackerDetailResDTO detail = TrackerConverter.toDetail(
                me,
                partner,
                TrackerDisplayStatus.READING,
                null,
                "my-profile",
                "partner-profile",
                List.of()
        );

        assertThat(detail.myBook().currentPage()).isEqualTo(30);
        assertThat(detail.myBook().currentReadingRate()).isEqualTo(30);
    }

    @Test
    void listAndDetailKeepOriginalDisplayWhenOnlyOneMemberRegisteredTracking() {
        TrackerResponses responses = convert(
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTER_WAITING
        );

        assertDisplayBooks(responses, "내 책", "상대 책", true, false);
    }

    @Test
    void listAndDetailSwapDisplayWhenBothMembersRegisteredTracking() {
        TrackerResponses responses = convert(
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTERED
        );

        assertDisplayBooks(responses, "상대 책", "내 책", false, true);
        assertDisplayProgress(responses, 0, 0);
        assertReaderPositions(responses);
    }

    @Test
    void waitingForPartnerReceiptShowsSwappedBooksWithZeroProgressInListAndDetail() {
        TrackerResponses responses = convert(
                ExchangeStatus.RECEIVED_CONFIRMED,
                ExchangeStatus.TRACKING_REGISTERED
        );

        assertDisplayBooks(responses, "상대 책", "내 책", false, true);
        assertDisplayProgress(responses, 0, 0);
        assertThat(responses.me().getCurrentMemberBook().getCurrentPage()).isEqualTo(100);
        assertThat(responses.partner().getCurrentMemberBook().getCurrentPage()).isEqualTo(100);
    }

    @Test
    void returnShippingShowsReturnedBooksWithoutResettingCompletedProgress() {
        TrackerResponses responses = convert(
                ReadingStatus.RETURNING,
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTERED
        );

        assertDisplayBooks(responses, "상대 책", "내 책", false, true);
        assertDisplayProgress(responses, 100, 100);
        assertReaderPositions(responses);
    }

    @Test
    void detailKeepsReaderPositionsForBothHostAndGuestAfterFirstExchange() {
        Groups group = Groups.builder()
                .id(2L)
                .groupName("직접 교환")
                .tradeType(TradeType.DIRECT)
                .build();
        MatchedMember guest = member(
                3L, 30L, "guest", group, RoleStatus.GUEST, ExchangeStatus.NOT_STARTED
        );
        MatchedMember host = member(
                4L, 40L, "host", group, RoleStatus.HOST, ExchangeStatus.NOT_STARTED
        );
        guest.updateReadingStatus(ReadingStatus.PARTNER_BOOK_READING);
        host.updateReadingStatus(ReadingStatus.PARTNER_BOOK_READING);
        MemberBook guestReadingHostBook = memberBook(3L, guest, "호스트 책", false);
        MemberBook hostReadingGuestBook = memberBook(4L, host, "게스트 책", false);
        guest.changeCurrentMemberBook(guestReadingHostBook, java.time.Instant.now());
        host.changeCurrentMemberBook(hostReadingGuestBook, java.time.Instant.now());
        guestReadingHostBook.updateCurrentPage(40);
        hostReadingGuestBook.updateCurrentPage(70);

        TrackerDetailResDTO guestView = TrackerConverter.toDetail(
                guest, host, TrackerDisplayStatus.READING, null,
                "guest-profile", "host-profile", List.of()
        );
        TrackerDetailResDTO hostView = TrackerConverter.toDetail(
                host, guest, TrackerDisplayStatus.READING, null,
                "host-profile", "guest-profile", List.of()
        );

        assertBookPosition(guestView, "호스트 책", "게스트 책", "guest", "host", false, true, 40);
        assertBookPosition(hostView, "게스트 책", "호스트 책", "host", "guest", false, true, 70);
    }

    private TrackerResponses convert(
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus
    ) {
        return convert(ReadingStatus.EXCHANGING, myExchangeStatus, partnerExchangeStatus);
    }

    private TrackerResponses convert(
            ReadingStatus readingStatus,
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus
    ) {
        Groups group = Groups.builder()
                .id(1L)
                .groupName("택배 교환")
                .tradeType(TradeType.DELIVERY)
                .build();
        MatchedMember me = member(1L, 10L, "me", group, RoleStatus.GUEST, myExchangeStatus);
        MatchedMember partner = member(2L, 20L, "partner", group, RoleStatus.HOST, partnerExchangeStatus);
        me.updateReadingStatus(readingStatus);
        partner.updateReadingStatus(readingStatus);
        MemberBook myBook = memberBook(1L, me, "내 책", true);
        MemberBook partnerBook = memberBook(2L, partner, "상대 책", true);
        me.changeCurrentMemberBook(myBook, java.time.Instant.now());
        partner.changeCurrentMemberBook(partnerBook, java.time.Instant.now());
        myBook.updateCurrentPage(100);
        partnerBook.updateCurrentPage(100);

        TrackerListItemResDTO list = TrackerConverter.toListItem(
                me,
                partner,
                TrackerDisplayStatus.SHIPPING,
                null,
                "my-profile",
                "partner-profile"
        );
        TrackerDetailResDTO detail = TrackerConverter.toDetail(
                me,
                partner,
                TrackerDisplayStatus.SHIPPING,
                null,
                "my-profile",
                "partner-profile",
                List.of()
        );
        return new TrackerResponses(list, detail, me, partner);
    }

    private MatchedMember member(
            Long memberId,
            Long userId,
            String nickname,
            Groups group,
            RoleStatus role,
            ExchangeStatus exchangeStatus
    ) {
        return MatchedMember.builder()
                .id(memberId)
                .group(group)
                .user(User.builder().id(userId).nickName(nickname).build())
                .role(role)
                .readingStatus(ReadingStatus.EXCHANGING)
                .exchangeStatus(exchangeStatus)
                .build();
    }

    private MemberBook memberBook(Long id, MatchedMember member, String title, boolean isMine) {
        MemberBook memberBook = MemberBook.builder()
                .id(id)
                .group(member.getGroup())
                .matchedMember(member)
                .book(Book.builder().id(id).title(title).totalPages(100).build())
                .isMine(isMine)
                .build();
        member.getMemberBooks().add(memberBook);
        return memberBook;
    }

    private void assertDisplayBooks(
            TrackerResponses responses,
            String expectedMyBookTitle,
            String expectedPartnerBookTitle,
            boolean expectedMyOriginalBook,
            boolean expectedPartnerOriginalBook
    ) {
        assertThat(responses.list().getMyCurrentBook().title()).isEqualTo(expectedMyBookTitle);
        assertThat(responses.list().getPartnerCurrentBook().title()).isEqualTo(expectedPartnerBookTitle);
        assertThat(responses.detail().myBook().title()).isEqualTo(expectedMyBookTitle);
        assertThat(responses.detail().partnerBook().title()).isEqualTo(expectedPartnerBookTitle);
        assertThat(responses.list().getDisplayBookTitle()).isEqualTo(expectedMyBookTitle);
        assertThat(responses.list().getDisplayBookTitle()).isEqualTo(responses.detail().displayBookTitle());
        assertThat(responses.list().getDisplayStatusLabel()).isEqualTo("배송 중");
        assertThat(responses.list().getDisplayStatusLabel()).isEqualTo(responses.detail().displayStatusLabel());

        assertThat(responses.list().getMyCurrentBook().isMyOriginalBook()).isEqualTo(expectedMyOriginalBook);
        assertThat(responses.list().getPartnerCurrentBook().isMyOriginalBook())
                .isEqualTo(expectedPartnerOriginalBook);
        assertThat(responses.detail().myBook().isMyOriginalBook()).isEqualTo(expectedMyOriginalBook);
        assertThat(responses.detail().partnerBook().isMyOriginalBook())
                .isEqualTo(expectedPartnerOriginalBook);
    }

    private void assertDisplayProgress(
            TrackerResponses responses,
            int expectedCurrentPage,
            int expectedReadingRate
    ) {
        assertThat(responses.list().getMyCurrentBook().currentPage()).isEqualTo(expectedCurrentPage);
        assertThat(responses.list().getMyCurrentBook().currentReadingRate()).isEqualTo(expectedReadingRate);
        assertThat(responses.detail().myBook().currentPage()).isEqualTo(expectedCurrentPage);
        assertThat(responses.detail().myBook().currentReadingRate()).isEqualTo(expectedReadingRate);
    }

    private void assertReaderPositions(TrackerResponses responses) {
        assertThat(responses.list().getMyCurrentBook().currentReaderNickname()).isEqualTo("me");
        assertThat(responses.list().getMyCurrentBook().currentReaderProfileImageUrl()).isEqualTo("my-profile");
        assertThat(responses.list().getPartnerCurrentBook().currentReaderNickname()).isEqualTo("partner");
        assertThat(responses.list().getPartnerCurrentBook().currentReaderProfileImageUrl())
                .isEqualTo("partner-profile");
        assertThat(responses.detail().myBook().currentReaderNickname()).isEqualTo("me");
        assertThat(responses.detail().myBook().currentReaderProfileImageUrl()).isEqualTo("my-profile");
        assertThat(responses.detail().partnerBook().currentReaderNickname()).isEqualTo("partner");
        assertThat(responses.detail().partnerBook().currentReaderProfileImageUrl()).isEqualTo("partner-profile");
    }

    private void assertBookPosition(
            TrackerDetailResDTO response,
            String myBookTitle,
            String partnerBookTitle,
            String myReader,
            String partnerReader,
            boolean myOriginalBook,
            boolean partnerOriginalBook,
            int myCurrentPage
    ) {
        assertThat(response.myBook().title()).isEqualTo(myBookTitle);
        assertThat(response.partnerBook().title()).isEqualTo(partnerBookTitle);
        assertThat(response.myBook().currentReaderNickname()).isEqualTo(myReader);
        assertThat(response.partnerBook().currentReaderNickname()).isEqualTo(partnerReader);
        assertThat(response.myBook().isMyOriginalBook()).isEqualTo(myOriginalBook);
        assertThat(response.partnerBook().isMyOriginalBook()).isEqualTo(partnerOriginalBook);
        assertThat(response.myBook().currentPage()).isEqualTo(myCurrentPage);
        assertThat(response.myBook().currentReadingRate()).isEqualTo(myCurrentPage);
    }

    private record TrackerResponses(
            TrackerListItemResDTO list,
            TrackerDetailResDTO detail,
            MatchedMember me,
            MatchedMember partner
    ) {
    }
}
