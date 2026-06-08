package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.util.TrackerTextFormatter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackerStepAssembler {

    /*
     * Tracker steps are returned in fixed chronological order:
     * first reading -> first review -> first exchange -> partner reading
     * -> second review -> return exchange -> partner review.
     */
    public List<TrackerStepInfo> assemble(MatchedMember me) {
        return assemble(me, false);
    }

    public List<TrackerStepInfo> assemble(MatchedMember me, boolean currentBookReviewWritten) {
        StepContext context = new StepContext(me.getReadingStatus(), me.getExchangeStatus(), currentBookReviewWritten, me.isReviewWritten());
        String myBookTitle = findMyBookTitle(me);
        String partnerBookTitle = findPartnerBookTitle(me);
        String abbreviatedMyBookTitle = TrackerTextFormatter.abbreviateStepBookTitle(myBookTitle);
        String abbreviatedPartnerBookTitle = TrackerTextFormatter.abbreviateStepBookTitle(partnerBookTitle);
        List<StepDefinition> definitions = me.getGroup().getTradeType() == TradeType.DELIVERY
                ? deliverySteps(abbreviatedMyBookTitle, abbreviatedPartnerBookTitle)
                : directSteps(abbreviatedMyBookTitle, abbreviatedPartnerBookTitle);

        return definitions.stream()
                .map(step -> TrackerStepInfo.builder()
                        .status(step.status())
                        .title(step.title())
                        .description(step.description())
                        .completed(isCompleted(step.type(), context))
                        .build())
                .toList();
    }

    private List<StepDefinition> directSteps(
            String abbreviatedMyBookTitle,
            String abbreviatedPartnerBookTitle
    ) {
        return List.of(
                step(StepType.MY_BOOK_READING, ReadingStatus.MY_BOOK_READING, abbreviatedMyBookTitle + " 읽기", "내 책을 읽고 독서 진행률을 기록해주세요"),
                step(StepType.MY_BOOK_REVIEW, ReadingStatus.MY_BOOK_REVIEWING, "책 후기 작성하기", abbreviatedMyBookTitle + "의 책 후기를 작성해주세요"),
                step(StepType.FIRST_MEETING_REGISTER, ReadingStatus.EXCHANGING, "교환 약속 등록하기", "파트너와 첫 교환 약속을 등록해주세요"),
                step(StepType.FIRST_BOOK_EXCHANGE, ReadingStatus.EXCHANGING, "책 교환하기", "약속 장소에서 책 교환 완료를 처리해주세요"),
                step(StepType.PARTNER_BOOK_READING, ReadingStatus.PARTNER_BOOK_READING, abbreviatedPartnerBookTitle + " 읽기", "상대 책을 읽고 독서 진행률을 기록해주세요"),
                step(StepType.PARTNER_BOOK_REVIEW, ReadingStatus.PARTNER_BOOK_REVIEWING, "책 후기 작성하기", abbreviatedPartnerBookTitle + "의 책 후기를 작성해주세요"),
                step(StepType.RETURN_MEETING_REGISTER, ReadingStatus.RETURNING, "반납 약속 등록하기", "파트너와 책 반납 약속을 등록해주세요"),
                step(StepType.RETURN_BOOK_EXCHANGE, ReadingStatus.RETURNING, "책 반납하기", "약속 장소에서 책 반납 완료를 처리해주세요"),
                step(StepType.PARTNER_REVIEW, ReadingStatus.PARTNER_REVIEWING, "교환독서 후기 작성하기", "함께한 파트너에 대한 후기를 작성해주세요")
        );
    }

    private List<StepDefinition> deliverySteps(
            String abbreviatedMyBookTitle,
            String abbreviatedPartnerBookTitle
    ) {
        return List.of(
                step(StepType.MY_BOOK_READING, ReadingStatus.MY_BOOK_READING, abbreviatedMyBookTitle + " 읽기", "내 책을 읽고 독서 진행률을 기록해주세요"),
                step(StepType.MY_BOOK_REVIEW, ReadingStatus.MY_BOOK_REVIEWING, "책 후기 작성하기", abbreviatedMyBookTitle + "의 책 후기를 작성해주세요"),
                step(StepType.FIRST_TRACKING_REGISTER, ReadingStatus.EXCHANGING, "운송장 등록하기", "파트너에게 보낸 책의 운송장을 등록해주세요"),
                step(StepType.FIRST_RECEIPT_CONFIRM, ReadingStatus.EXCHANGING, "수령 인증 확인하기", "파트너가 보낸 책을 받은 뒤 수령 인증을 등록해주세요"),
                step(StepType.PARTNER_BOOK_READING, ReadingStatus.PARTNER_BOOK_READING, abbreviatedPartnerBookTitle + " 읽기", "상대 책을 읽고 독서 진행률을 기록해주세요"),
                step(StepType.PARTNER_BOOK_REVIEW, ReadingStatus.PARTNER_BOOK_REVIEWING, "책 후기 작성하기", abbreviatedPartnerBookTitle + "의 책 후기를 작성해주세요"),
                step(StepType.RETURN_TRACKING_REGISTER, ReadingStatus.RETURNING, "반납 운송장 등록하기", "파트너에게 반납한 책의 운송장을 등록해주세요"),
                step(StepType.RETURN_RECEIPT_CONFIRM, ReadingStatus.RETURNING, "수령 인증 확인하기", "돌려받은 책을 확인한 뒤 수령 인증을 등록해주세요"),
                step(StepType.PARTNER_REVIEW, ReadingStatus.PARTNER_REVIEWING, "교환독서 후기 작성하기", "함께한 파트너에 대한 후기를 작성해주세요")
        );
    }

    private boolean isCompleted(StepType stepType, StepContext context) {
        ReadingStatus readingStatus = context.readingStatus();
        ExchangeStatus exchangeStatus = context.exchangeStatus();

        return switch (stepType) {
            case MY_BOOK_READING -> isAfter(readingStatus, ReadingStatus.MY_BOOK_READING);
            case MY_BOOK_REVIEW -> isAfter(readingStatus, ReadingStatus.MY_BOOK_REVIEWING)
                    || (readingStatus == ReadingStatus.MY_BOOK_REVIEWING && context.currentBookReviewWritten());
            case FIRST_MEETING_REGISTER -> isAfter(readingStatus, ReadingStatus.EXCHANGING)
                    || (readingStatus == ReadingStatus.EXCHANGING && isMeetingScheduled(exchangeStatus));
            case FIRST_BOOK_EXCHANGE -> isAfter(readingStatus, ReadingStatus.EXCHANGING)
                    || (readingStatus == ReadingStatus.EXCHANGING && exchangeStatus == ExchangeStatus.MEETING_COMPLETED);
            case FIRST_TRACKING_REGISTER -> isAfter(readingStatus, ReadingStatus.EXCHANGING)
                    || (readingStatus == ReadingStatus.EXCHANGING && isTrackingRegistered(exchangeStatus));
            case FIRST_RECEIPT_CONFIRM -> isAfter(readingStatus, ReadingStatus.EXCHANGING)
                    || (readingStatus == ReadingStatus.EXCHANGING && exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED);
            case PARTNER_BOOK_READING -> isAfter(readingStatus, ReadingStatus.PARTNER_BOOK_READING);
            case PARTNER_BOOK_REVIEW -> isAfter(readingStatus, ReadingStatus.PARTNER_BOOK_REVIEWING)
                    || (readingStatus == ReadingStatus.PARTNER_BOOK_REVIEWING && context.currentBookReviewWritten());
            case RETURN_MEETING_REGISTER -> isAfter(readingStatus, ReadingStatus.RETURNING)
                    || (readingStatus == ReadingStatus.RETURNING && isMeetingScheduled(exchangeStatus));
            case RETURN_BOOK_EXCHANGE -> isAfter(readingStatus, ReadingStatus.RETURNING)
                    || (readingStatus == ReadingStatus.RETURNING && exchangeStatus == ExchangeStatus.MEETING_COMPLETED);
            case RETURN_TRACKING_REGISTER -> isAfter(readingStatus, ReadingStatus.RETURNING)
                    || (readingStatus == ReadingStatus.RETURNING && isTrackingRegistered(exchangeStatus));
            case RETURN_RECEIPT_CONFIRM -> isAfter(readingStatus, ReadingStatus.RETURNING)
                    || (readingStatus == ReadingStatus.RETURNING && exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED);
            case PARTNER_REVIEW -> context.partnerReviewWritten();
        };
    }

    private boolean isMeetingScheduled(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.MEETING_SCHEDULED
                || exchangeStatus == ExchangeStatus.MEETING_COMPLETED;
    }

    private boolean isTrackingRegistered(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.TRACKING_REGISTERED
                || exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED;
    }

    private boolean isAfter(ReadingStatus current, ReadingStatus reference) {
        return order(current) > order(reference);
    }

    private int order(ReadingStatus status) {
        return switch (status) {
            case MY_BOOK_READING -> 1;
            case MY_BOOK_REVIEWING -> 2;
            case EXCHANGING -> 3;
            case PARTNER_BOOK_READING -> 4;
            case PARTNER_BOOK_REVIEWING -> 5;
            case RETURNING -> 6;
            case PARTNER_REVIEWING -> 7;
            case COMPLETED -> 8;
        };
    }

    private StepDefinition step(StepType type, ReadingStatus status, String title, String description) {
        return new StepDefinition(type, status, title, description);
    }

    private String findMyBookTitle(MatchedMember me) {
        return me.getMemberBooks().stream()
                .filter(MemberBook::isMine)
                .findFirst()
                .map(memberBook -> memberBook.getBook().getTitle())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));
    }

    private String findPartnerBookTitle(MatchedMember me) {
        return me.getMemberBooks().stream()
                .filter(memberBook -> !memberBook.isMine())
                .findFirst()
                .map(memberBook -> memberBook.getBook().getTitle())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));
    }

    private enum StepType {
        MY_BOOK_READING,
        MY_BOOK_REVIEW,
        FIRST_MEETING_REGISTER,
        FIRST_BOOK_EXCHANGE,
        FIRST_TRACKING_REGISTER,
        FIRST_RECEIPT_CONFIRM,
        PARTNER_BOOK_READING,
        PARTNER_BOOK_REVIEW,
        RETURN_MEETING_REGISTER,
        RETURN_BOOK_EXCHANGE,
        RETURN_TRACKING_REGISTER,
        RETURN_RECEIPT_CONFIRM,
        PARTNER_REVIEW
    }

    private record StepContext(
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus,
            boolean currentBookReviewWritten,
            boolean partnerReviewWritten
    ) {}

    private record StepDefinition(
            StepType type,
            ReadingStatus status,
            String title,
            String description
    ) {}
}
