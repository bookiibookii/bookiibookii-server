package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import org.springframework.stereotype.Component;

@Component
public class TrackerDisplayStatusResolver {

    public TrackerDisplayStatus resolve(
            MatchedMember me,
            MatchedMember partner,
            boolean currentBookReviewWritten
    ) {
        return resolve(
                me.getReadingStatus(),
                me.getExchangeStatus(),
                partner == null ? null : partner.getExchangeStatus(),
                me.getGroup().getTradeType(),
                me.getRole(),
                currentBookReviewWritten,
                me.isReviewWritten()
        );
    }

    public TrackerDisplayStatus resolve(
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus,
            TradeType tradeType,
            boolean currentBookReviewWritten
    ) {
        return resolve(
                readingStatus,
                exchangeStatus,
                null,
                tradeType,
                RoleStatus.HOST,
                currentBookReviewWritten,
                false
        );
    }

    public TrackerDisplayStatus resolve(
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus,
            TradeType tradeType
    ) {
        return resolve(readingStatus, exchangeStatus, tradeType, false);
    }

    public TrackerDisplayStatus resolve(
            ReadingStatus readingStatus,
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus,
            TradeType tradeType,
            RoleStatus myRole,
            boolean currentBookReviewWritten
    ) {
        return resolve(
                readingStatus,
                myExchangeStatus,
                partnerExchangeStatus,
                tradeType,
                myRole,
                currentBookReviewWritten,
                false
        );
    }

    public TrackerDisplayStatus resolve(
            ReadingStatus readingStatus,
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus,
            TradeType tradeType,
            RoleStatus myRole,
            boolean currentBookReviewWritten,
            boolean exchangeReviewWritten
    ) {
        if (readingStatus == null) {
            return TrackerDisplayStatus.READING;
        }

        return switch (readingStatus) {
            case MY_BOOK_READING, PARTNER_BOOK_READING -> TrackerDisplayStatus.READING;
            case MY_BOOK_REVIEWING, PARTNER_BOOK_REVIEWING -> currentBookReviewWritten
                    ? TrackerDisplayStatus.REVIEW_WAITING_PARTNER
                    : TrackerDisplayStatus.REVIEW_WRITING;
            case EXCHANGING -> resolveExchangeStatus(myExchangeStatus, partnerExchangeStatus, tradeType, myRole, false);
            case RETURNING -> resolveExchangeStatus(myExchangeStatus, partnerExchangeStatus, tradeType, myRole, true);
            case PARTNER_REVIEWING -> exchangeReviewWritten
                    ? TrackerDisplayStatus.EXCHANGE_REVIEW_WAITING_PARTNER
                    : TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;
            case COMPLETED -> TrackerDisplayStatus.COMPLETED;
        };
    }

    private TrackerDisplayStatus resolveExchangeStatus(
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus,
            TradeType tradeType,
            RoleStatus myRole,
            boolean returnExchange
    ) {
        if (tradeType == TradeType.DELIVERY) {
            return resolveDeliveryDisplay(myExchangeStatus, partnerExchangeStatus, returnExchange);
        }
        return resolveDirectStatus(myExchangeStatus, partnerExchangeStatus, myRole);
    }

    private TrackerDisplayStatus resolveDeliveryDisplay(
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus,
            boolean returnExchange
    ) {
        ExchangeStatus mine = safeExchangeStatus(myExchangeStatus);
        ExchangeStatus partner = safeExchangeStatus(partnerExchangeStatus);

        if (mine == ExchangeStatus.RECEIVED_CONFIRMED && partner != ExchangeStatus.RECEIVED_CONFIRMED) {
            return TrackerDisplayStatus.WAITING_PARTNER_RECEIPT_CONFIRM;
        }
        if (mine == ExchangeStatus.TRACKING_REGISTERED
                && (partner == ExchangeStatus.TRACKING_REGISTER_WAITING || partner == ExchangeStatus.NOT_STARTED)) {
            return TrackerDisplayStatus.WAITING_PARTNER_TRACKING_REGISTER;
        }

        return switch (mine) {
            case TRACKING_REGISTER_WAITING, NOT_STARTED -> returnExchange
                    ? TrackerDisplayStatus.RETURN_TRACKING_REQUIRED
                    : TrackerDisplayStatus.TRACKING_REQUIRED;
            case TRACKING_REGISTERED -> returnExchange
                    ? TrackerDisplayStatus.RETURNING
                    : TrackerDisplayStatus.SHIPPING;
            case RECEIVED_CONFIRMED -> returnExchange
                    ? TrackerDisplayStatus.RETURNING
                    : TrackerDisplayStatus.SHIPPING;
            default -> returnExchange
                    ? TrackerDisplayStatus.RETURN_TRACKING_REQUIRED
                    : TrackerDisplayStatus.TRACKING_REQUIRED;
        };
    }

    private TrackerDisplayStatus resolveDirectStatus(
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus,
            RoleStatus myRole
    ) {
        ExchangeStatus mine = safeExchangeStatus(myExchangeStatus);
        ExchangeStatus partner = safeExchangeStatus(partnerExchangeStatus);

        if (mine == ExchangeStatus.MEETING_COMPLETED && partner != ExchangeStatus.MEETING_COMPLETED) {
            return TrackerDisplayStatus.WAITING_PARTNER_MEETING_COMPLETE;
        }

        return switch (mine) {
            case MEETING_SCHEDULED, MEETING_COMPLETED -> TrackerDisplayStatus.EXCHANGING;
            case MEETING_SCHEDULE_WAITING, NOT_STARTED -> myRole == RoleStatus.HOST
                    ? TrackerDisplayStatus.MEETING_REGISTER_REQUIRED
                    : TrackerDisplayStatus.WAITING_HOST_MEETING_REGISTER;
            default -> myRole == RoleStatus.HOST
                    ? TrackerDisplayStatus.MEETING_REGISTER_REQUIRED
                    : TrackerDisplayStatus.WAITING_HOST_MEETING_REGISTER;
        };
    }

    private ExchangeStatus safeExchangeStatus(ExchangeStatus exchangeStatus) {
        return exchangeStatus == null ? ExchangeStatus.NOT_STARTED : exchangeStatus;
    }
}
