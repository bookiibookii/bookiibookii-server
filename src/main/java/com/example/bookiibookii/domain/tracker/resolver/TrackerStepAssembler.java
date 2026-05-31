package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackerStepAssembler {

    public List<TrackerStepInfo> assemble(MatchedMember me) {
        TradeType tradeType = me.getGroup().getTradeType();
        ReadingStatus currentStatus = me.getReadingStatus();
        ExchangeStatus exchangeStatus = me.getExchangeStatus();

        String myBookTitle = findMyBookTitle(me);
        String partnerBookTitle = findPartnerBookTitle(me);

        List<TrackerStepInfo> steps = tradeType == TradeType.DELIVERY
                ? deliverySteps(myBookTitle, partnerBookTitle)
                : directSteps(myBookTitle, partnerBookTitle);

        return steps.stream()
                .map(step -> TrackerStepInfo.builder()
                        .status(step.status())
                        .title(step.title())
                        .description(step.description())
                        .completed(isCompleted(currentStatus, exchangeStatus, step.status(), tradeType))
                        .build())
                .toList();
    }

    private List<TrackerStepInfo> deliverySteps(String myBookTitle, String partnerBookTitle) {
        return List.of(
                step(
                        ReadingStatus.MY_BOOK_READING,
                        myBookTitle + " 읽기",
                        "독서카드를 작성하면 교환독서가 더 즐거워져요"
                ),
                step(
                        ReadingStatus.MY_BOOK_REVIEWING,
                        "책 리뷰 작성",
                        myBookTitle + "의 리뷰를 작성해주세요"
                ),
                step(
                        ReadingStatus.EXCHANGING,
                        "1차 교환하기",
                        "파트너에게 " + myBookTitle + "을 발송해주세요"
                ),
                step(
                        ReadingStatus.EXCHANGED,
                        "수령 인증 등록",
                        "파트너가 보낸 책 상태를 확인해주세요"
                ),
                step(
                        ReadingStatus.PARTNER_BOOK_READING,
                        partnerBookTitle + " 읽기",
                        "독서카드를 작성하면 교환독서가 더 즐거워져요"
                ),
                step(
                        ReadingStatus.PARTNER_BOOK_REVIEWING,
                        "책 리뷰 작성",
                        partnerBookTitle + "의 리뷰를 작성해주세요"
                ),
                step(
                        ReadingStatus.RETURNING,
                        "2차 교환하기",
                        "파트너에게 " + partnerBookTitle + "을 발송해주세요"
                ),
                step(
                        ReadingStatus.RETURNED,
                        "수령 인증 등록",
                        "파트너가 보낸 책 상태를 확인해주세요"
                ),
                step(
                        ReadingStatus.COMPLETED,
                        "교환독서 종료",
                        "소중한 교환 후기를 남겨주세요"
                )
        );
    }

    private List<TrackerStepInfo> directSteps(String myBookTitle, String partnerBookTitle) {
        return List.of(
                step(
                        ReadingStatus.MY_BOOK_READING,
                        myBookTitle + " 읽기",
                        "독서카드를 작성하면 교환독서가 더 즐거워져요"
                ),
                step(
                        ReadingStatus.MY_BOOK_REVIEWING,
                        "책 리뷰 작성",
                        myBookTitle + "의 리뷰를 작성해주세요"
                ),
                step(
                        ReadingStatus.EXCHANGING,
                        "1차 교환하기",
                        "파트너와 논의 후 약속을 확정해주세요"
                ),
                step(
                        ReadingStatus.EXCHANGED,
                        "교환 인증 등록",
                        "반드시 파트너와 책을 실제로 교환한 뒤 진행해주세요"
                ),
                step(
                        ReadingStatus.PARTNER_BOOK_READING,
                        partnerBookTitle + " 읽기",
                        "독서카드를 작성하면 교환독서가 더 즐거워져요"
                ),
                step(
                        ReadingStatus.PARTNER_BOOK_REVIEWING,
                        "책 리뷰 작성",
                        partnerBookTitle + "의 리뷰를 작성해주세요"
                ),
                step(
                        ReadingStatus.RETURNING,
                        "2차 교환하기",
                        "파트너와 논의 후 약속을 확정해주세요"
                ),
                step(
                        ReadingStatus.RETURNED,
                        "교환 인증 등록",
                        "반드시 파트너와 책을 실제로 교환한 뒤 진행해주세요"
                ),
                step(
                        ReadingStatus.COMPLETED,
                        "교환독서 종료",
                        "소중한 교환 후기를 남겨주세요"
                )
        );
    }

    private TrackerStepInfo step(ReadingStatus status, String title, String description) {
        return TrackerStepInfo.builder()
                .status(status)
                .title(title)
                .description(description)
                .completed(false)
                .build();
    }

    private boolean isCompleted(
            ReadingStatus currentStatus,
            ExchangeStatus exchangeStatus,
            ReadingStatus stepStatus,
            TradeType tradeType
    ) {
        if (currentStatus == ReadingStatus.COMPLETED) {
            return true;
        }
        if (currentStatus == ReadingStatus.EXCHANGING) {
            return isFirstExchangeStepCompleted(exchangeStatus, stepStatus, tradeType);
        }
        if (currentStatus == ReadingStatus.RETURNING) {
            return isReturnExchangeStepCompleted(exchangeStatus, stepStatus, tradeType);
        }
        return resolveOrder(currentStatus) > resolveOrder(stepStatus);
    }

    private boolean isFirstExchangeStepCompleted(
            ExchangeStatus exchangeStatus,
            ReadingStatus stepStatus,
            TradeType tradeType
    ) {
        if (stepStatus == ReadingStatus.EXCHANGING) {
            return tradeType == TradeType.DELIVERY
                    ? isTrackingRegistered(exchangeStatus)
                    : isMeetingScheduled(exchangeStatus);
        }
        if (stepStatus == ReadingStatus.EXCHANGED) {
            return tradeType == TradeType.DELIVERY
                    ? isReceivedConfirmed(exchangeStatus)
                    : isMeetingCompleted(exchangeStatus);
        }
        return resolveOrder(ReadingStatus.EXCHANGING) > resolveOrder(stepStatus);
    }

    private boolean isReturnExchangeStepCompleted(
            ExchangeStatus exchangeStatus,
            ReadingStatus stepStatus,
            TradeType tradeType
    ) {
        if (stepStatus == ReadingStatus.RETURNING) {
            return tradeType == TradeType.DELIVERY
                    ? isTrackingRegistered(exchangeStatus)
                    : isMeetingScheduled(exchangeStatus);
        }
        if (stepStatus == ReadingStatus.RETURNED) {
            return tradeType == TradeType.DELIVERY
                    ? isReceivedConfirmed(exchangeStatus)
                    : isMeetingCompleted(exchangeStatus);
        }
        return resolveOrder(ReadingStatus.RETURNING) > resolveOrder(stepStatus);
    }

    private boolean isTrackingRegistered(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.TRACKING_REGISTERED
                || exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED;
    }

    private boolean isReceivedConfirmed(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED;
    }

    private boolean isMeetingScheduled(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.MEETING_SCHEDULED
                || exchangeStatus == ExchangeStatus.MEETING_COMPLETED;
    }

    private boolean isMeetingCompleted(ExchangeStatus exchangeStatus) {
        return exchangeStatus == ExchangeStatus.MEETING_COMPLETED;
    }

    private int resolveOrder(ReadingStatus status) {
        return switch (status) {
            case MY_BOOK_READING -> 1;
            case MY_BOOK_REVIEWING -> 2;
            case EXCHANGING -> 3;
            case EXCHANGED -> 4;
            case PARTNER_BOOK_READING -> 5;
            case PARTNER_BOOK_REVIEWING -> 6;
            case RETURNING -> 7;
            case RETURNED -> 8;
            case COMPLETED -> 9;
        };
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
}
