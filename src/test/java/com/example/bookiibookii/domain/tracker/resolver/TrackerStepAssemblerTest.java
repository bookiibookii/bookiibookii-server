package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerStepStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerStepAssemblerTest {

    private final TrackerStepAssembler assembler = new TrackerStepAssembler();

    @Test
    void directStepsUseFixedChronologicalOrderAndCurrentPolicyTitles() {
        List<TrackerStepInfo> steps = assembler.assemble(member(
                TradeType.DIRECT,
                ReadingStatus.MY_BOOK_READING,
                ExchangeStatus.NOT_STARTED
        ));

        assertThat(steps).extracting(TrackerStepInfo::title)
                .containsExactly(
                        "내 책 읽기",
                        "책 후기 작성하기",
                        "교환 약속 등록하기",
                        "책 교환하기",
                        "상대 책 읽기",
                        "책 후기 작성하기",
                        "반납 약속 등록하기",
                        "책 반납하기",
                        "교환독서 후기 작성하기"
                );
        assertThat(steps).extracting(TrackerStepInfo::status)
                .containsExactly(
                        TrackerStepStatus.MY_BOOK_READING,
                        TrackerStepStatus.MY_BOOK_REVIEWING,
                        TrackerStepStatus.DIRECT_EXCHANGE_MEETING_REGISTER,
                        TrackerStepStatus.DIRECT_EXCHANGE_COMPLETE,
                        TrackerStepStatus.PARTNER_BOOK_READING,
                        TrackerStepStatus.PARTNER_BOOK_REVIEWING,
                        TrackerStepStatus.DIRECT_RETURN_MEETING_REGISTER,
                        TrackerStepStatus.DIRECT_RETURN_COMPLETE,
                        TrackerStepStatus.PARTNER_REVIEWING
                );
    }

    @Test
    void deliveryStepsUseFixedChronologicalOrderAndCurrentPolicyTitles() {
        List<TrackerStepInfo> steps = assembler.assemble(member(
                TradeType.DELIVERY,
                ReadingStatus.MY_BOOK_READING,
                ExchangeStatus.NOT_STARTED
        ));

        assertThat(steps).extracting(TrackerStepInfo::title)
                .containsExactly(
                        "내 책 읽기",
                        "책 후기 작성하기",
                        "운송장 등록하기",
                        "수령 인증 확인하기",
                        "상대 책 읽기",
                        "책 후기 작성하기",
                        "반납 운송장 등록하기",
                        "수령 인증 확인하기",
                        "교환독서 후기 작성하기"
                );
        assertThat(steps).extracting(TrackerStepInfo::status)
                .containsExactly(
                        TrackerStepStatus.MY_BOOK_READING,
                        TrackerStepStatus.MY_BOOK_REVIEWING,
                        TrackerStepStatus.EXCHANGE_TRACKING_REGISTER,
                        TrackerStepStatus.EXCHANGE_RECEIPT_CONFIRM,
                        TrackerStepStatus.PARTNER_BOOK_READING,
                        TrackerStepStatus.PARTNER_BOOK_REVIEWING,
                        TrackerStepStatus.RETURN_TRACKING_REGISTER,
                        TrackerStepStatus.RETURN_RECEIPT_CONFIRM,
                        TrackerStepStatus.PARTNER_REVIEWING
                );
    }

    @Test
    void keepsBookTitleInStepTextWhenTitleIsSixteenCodePointsOrShorter() {
        MatchedMember member = member(
                TradeType.DIRECT,
                ReadingStatus.MY_BOOK_READING,
                ExchangeStatus.NOT_STARTED,
                "1234567890123456",
                "상대 책"
        );

        List<TrackerStepInfo> steps = assembler.assemble(member);

        assertThat(steps.get(0).title()).isEqualTo("1234567890123456 읽기");
        assertThat(steps.get(1).description()).isEqualTo("1234567890123456의 책 후기를 작성해주세요");
    }

    @Test
    void abbreviatesMyBookTitleInStepTitleAndDescriptionWhenTitleIsLongerThanSixteenCodePoints() {
        MatchedMember member = member(
                TradeType.DIRECT,
                ReadingStatus.MY_BOOK_READING,
                ExchangeStatus.NOT_STARTED,
                "12345678901234567",
                "상대 책"
        );

        List<TrackerStepInfo> steps = assembler.assemble(member);

        assertThat(steps.get(0).title()).isEqualTo("1234567890123456... 읽기");
        assertThat(steps.get(1).description()).isEqualTo("1234567890123456...의 책 후기를 작성해주세요");
    }

    @Test
    void countsSpacesWhenAbbreviatingBookTitleInStepText() {
        MatchedMember member = member(
                TradeType.DIRECT,
                ReadingStatus.MY_BOOK_READING,
                ExchangeStatus.NOT_STARTED,
                "아무도 미워하지 않고 한 계절이 지나갔다",
                "상대 책"
        );

        List<TrackerStepInfo> steps = assembler.assemble(member);

        assertThat(steps.get(0).title()).isEqualTo("아무도 미워하지 않고 한 계절... 읽기");
        assertThat(steps.get(1).description()).isEqualTo("아무도 미워하지 않고 한 계절...의 책 후기를 작성해주세요");
    }

    @Test
    void abbreviatesPartnerBookTitleInStepTitleAndDescriptionWhenTitleIsLongerThanSixteenCodePoints() {
        MatchedMember member = member(
                TradeType.DIRECT,
                ReadingStatus.PARTNER_BOOK_READING,
                ExchangeStatus.NOT_STARTED,
                "내 책",
                "아몬드 (양장 특별 한정판) - 제10회 창비 청소년문학상 수상작"
        );

        List<TrackerStepInfo> steps = assembler.assemble(member);

        assertThat(steps.get(4).title()).isEqualTo("아몬드 (양장 특별 한정판) ... 읽기");
        assertThat(steps.get(5).description()).isEqualTo("아몬드 (양장 특별 한정판) ...의 책 후기를 작성해주세요");
    }

    @Test
    void directFirstExchangeSeparatesMeetingRegistrationAndBookExchange() {
        List<TrackerStepInfo> beforeMeeting = assembler.assemble(member(
                TradeType.DIRECT,
                ReadingStatus.EXCHANGING,
                ExchangeStatus.MEETING_SCHEDULE_WAITING
        ));
        assertThat(step(beforeMeeting, "교환 약속 등록하기").completed()).isFalse();
        assertThat(step(beforeMeeting, "책 교환하기").completed()).isFalse();

        List<TrackerStepInfo> afterMeeting = assembler.assemble(member(
                TradeType.DIRECT,
                ReadingStatus.EXCHANGING,
                ExchangeStatus.MEETING_SCHEDULED
        ));
        assertThat(step(afterMeeting, "교환 약속 등록하기").completed()).isTrue();
        assertThat(step(afterMeeting, "책 교환하기").completed()).isFalse();
    }

    @Test
    void deliveryFirstExchangeSeparatesTrackingRegistrationAndReceiptConfirmation() {
        List<TrackerStepInfo> beforeTracking = assembler.assemble(member(
                TradeType.DELIVERY,
                ReadingStatus.EXCHANGING,
                ExchangeStatus.TRACKING_REGISTER_WAITING
        ));
        assertThat(step(beforeTracking, "운송장 등록하기").completed()).isFalse();
        assertThat(step(beforeTracking, "수령 인증 확인하기").completed()).isFalse();
        assertThat(step(beforeTracking, "운송장 등록하기").status())
                .isEqualTo(TrackerStepStatus.EXCHANGE_TRACKING_REGISTER);
        assertThat(step(beforeTracking, "수령 인증 확인하기").status())
                .isEqualTo(TrackerStepStatus.EXCHANGE_RECEIPT_CONFIRM);

        List<TrackerStepInfo> afterTracking = assembler.assemble(member(
                TradeType.DELIVERY,
                ReadingStatus.EXCHANGING,
                ExchangeStatus.TRACKING_REGISTERED
        ));
        assertThat(step(afterTracking, "운송장 등록하기").completed()).isTrue();
        assertThat(step(afterTracking, "수령 인증 확인하기").completed()).isFalse();
    }

    @Test
    void deliveryReturnExchangeSeparatesTrackingRegistrationAndReceiptConfirmationStatuses() {
        List<TrackerStepInfo> steps = assembler.assemble(member(
                TradeType.DELIVERY,
                ReadingStatus.RETURNING,
                ExchangeStatus.TRACKING_REGISTER_WAITING
        ));

        assertThat(step(steps, TrackerStepStatus.RETURN_TRACKING_REGISTER).title())
                .isEqualTo("반납 운송장 등록하기");
        assertThat(step(steps, TrackerStepStatus.RETURN_RECEIPT_CONFIRM).title())
                .isEqualTo("수령 인증 확인하기");
        assertThat(step(steps, TrackerStepStatus.RETURN_TRACKING_REGISTER).completed()).isFalse();
        assertThat(step(steps, TrackerStepStatus.RETURN_RECEIPT_CONFIRM).completed()).isFalse();
    }

    @Test
    void directExchangeAndReturnStepsHaveDedicatedStatuses() {
        List<TrackerStepInfo> steps = assembler.assemble(member(
                TradeType.DIRECT,
                ReadingStatus.EXCHANGING,
                ExchangeStatus.MEETING_SCHEDULE_WAITING
        ));

        assertThat(step(steps, "교환 약속 등록하기").status())
                .isEqualTo(TrackerStepStatus.DIRECT_EXCHANGE_MEETING_REGISTER);
        assertThat(step(steps, "책 교환하기").status())
                .isEqualTo(TrackerStepStatus.DIRECT_EXCHANGE_COMPLETE);
        assertThat(step(steps, "반납 약속 등록하기").status())
                .isEqualTo(TrackerStepStatus.DIRECT_RETURN_MEETING_REGISTER);
        assertThat(step(steps, "책 반납하기").status())
                .isEqualTo(TrackerStepStatus.DIRECT_RETURN_COMPLETE);
    }

    @Test
    void partnerReviewStepDependsOnMyPartnerReviewFlag() {
        MatchedMember beforeReview = member(
                TradeType.DIRECT,
                ReadingStatus.PARTNER_REVIEWING,
                ExchangeStatus.NOT_STARTED
        );
        assertThat(step(assembler.assemble(beforeReview), "교환독서 후기 작성하기").completed()).isFalse();

        MatchedMember afterReview = member(
                TradeType.DIRECT,
                ReadingStatus.PARTNER_REVIEWING,
                ExchangeStatus.NOT_STARTED
        );
        afterReview.markReviewAsWritten();
        TrackerStepInfo completedStep = step(assembler.assemble(afterReview), "교환독서 후기 작성하기");
        assertThat(completedStep.completed()).isTrue();
        assertThat(completedStep.description())
                .isEqualTo("파트너가 교환독서 후기를 작성하면 교환독서가 종료돼요.");
        assertThat(afterReview.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
    }

    @Test
    void inProgressGroupDoesNotMarkEveryStepCompleted() {
        List<TrackerStepInfo> steps = assembler.assemble(member(
                TradeType.DIRECT,
                ReadingStatus.RETURNING,
                ExchangeStatus.MEETING_SCHEDULED
        ));

        assertThat(steps).anyMatch(step -> !step.completed());
    }

    private TrackerStepInfo step(List<TrackerStepInfo> steps, String title) {
        return steps.stream()
                .filter(step -> step.title().equals(title))
                .findFirst()
                .orElseThrow();
    }

    private TrackerStepInfo step(List<TrackerStepInfo> steps, TrackerStepStatus status) {
        return steps.stream()
                .filter(step -> step.status() == status)
                .findFirst()
                .orElseThrow();
    }

    private MatchedMember member(TradeType tradeType, ReadingStatus readingStatus, ExchangeStatus exchangeStatus) {
        return member(tradeType, readingStatus, exchangeStatus, "내 책", "상대 책");
    }

    private MatchedMember member(
            TradeType tradeType,
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus,
            String myBookTitle,
            String partnerBookTitle
    ) {
        Groups group = Groups.builder().tradeType(tradeType).build();
        MatchedMember member = MatchedMember.builder()
                .group(group)
                .readingStatus(readingStatus)
                .exchangeStatus(exchangeStatus)
                .build();
        member.getMemberBooks().add(memberBook(member, myBookTitle, true));
        member.getMemberBooks().add(memberBook(member, partnerBookTitle, false));
        return member;
    }

    private MemberBook memberBook(MatchedMember member, String title, boolean isMine) {
        return MemberBook.builder()
                .matchedMember(member)
                .book(Book.builder().title(title).build())
                .isMine(isMine)
                .build();
    }
}
