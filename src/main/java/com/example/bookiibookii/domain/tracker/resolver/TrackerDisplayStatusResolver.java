package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import org.springframework.stereotype.Component;

@Component
public class TrackerDisplayStatusResolver {

    public TrackerDisplayStatus resolve(
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus,
            TradeType tradeType,
            boolean currentBookReviewWritten
    ) {
        if (readingStatus == null) {
            return TrackerDisplayStatus.READING;
        }

        return switch (readingStatus) {
            case MY_BOOK_READING, PARTNER_BOOK_READING ->
                    TrackerDisplayStatus.READING;

            case MY_BOOK_REVIEWING, PARTNER_BOOK_REVIEWING ->
                    currentBookReviewWritten
                            ? TrackerDisplayStatus.REVIEW_WAITING_PARTNER
                            : TrackerDisplayStatus.REVIEW_WRITING;

            case EXCHANGING ->
                    resolveFirstExchangeStatus(exchangeStatus, tradeType);

            case RETURNING ->
                    resolveReturnExchangeStatus(exchangeStatus, tradeType);

            case RETURNED ->
                    TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;

            case EXCHANGED ->
                    TrackerDisplayStatus.READING;

            case COMPLETED ->
                    TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;
        };
    }

    public TrackerDisplayStatus resolve(
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus,
            TradeType tradeType
    ) {
        return resolve(readingStatus, exchangeStatus, tradeType, false);
    }

    private TrackerDisplayStatus resolveFirstExchangeStatus(
            ExchangeStatus exchangeStatus,
            TradeType tradeType
    ) {
        if (tradeType == TradeType.DELIVERY) {
            return switch (safeExchangeStatus(exchangeStatus)) {
                case TRACKING_REGISTER_WAITING, NOT_STARTED ->
                        TrackerDisplayStatus.TRACKING_REQUIRED;

                case TRACKING_REGISTERED ->
                        TrackerDisplayStatus.SHIPPING;

                case RECEIVED_CONFIRMED ->
                        TrackerDisplayStatus.READING;

                default ->
                        TrackerDisplayStatus.TRACKING_REQUIRED;
            };
        }

        return switch (safeExchangeStatus(exchangeStatus)) {
            case MEETING_SCHEDULE_WAITING, NOT_STARTED ->
                    TrackerDisplayStatus.MEETING_REQUIRED;

            case MEETING_SCHEDULED, MEETING_COMPLETED ->
                    TrackerDisplayStatus.EXCHANGING;

            case MEETING_FAILED ->
                    TrackerDisplayStatus.MEETING_REQUIRED;

            default ->
                    TrackerDisplayStatus.MEETING_REQUIRED;
        };
    }

    private TrackerDisplayStatus resolveReturnExchangeStatus(
            ExchangeStatus exchangeStatus,
            TradeType tradeType
    ) {
        if (tradeType == TradeType.DELIVERY) {
            return switch (safeExchangeStatus(exchangeStatus)) {
                case TRACKING_REGISTER_WAITING ->
                        TrackerDisplayStatus.RETURN_TRACKING_REQUIRED;

                case TRACKING_REGISTERED ->
                        TrackerDisplayStatus.RETURNING;

                case RECEIVED_CONFIRMED, NOT_STARTED ->
                        TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;

                default ->
                        TrackerDisplayStatus.RETURN_TRACKING_REQUIRED;
            };
        }

        return switch (safeExchangeStatus(exchangeStatus)) {

            case MEETING_SCHEDULED, MEETING_COMPLETED ->
                    TrackerDisplayStatus.EXCHANGING;

            case NOT_STARTED ->
                    TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;

            default ->
                    TrackerDisplayStatus.MEETING_REQUIRED;
        };
    }

    private ExchangeStatus safeExchangeStatus(ExchangeStatus exchangeStatus) {
        return exchangeStatus == null ? ExchangeStatus.NOT_STARTED : exchangeStatus;
    }
}
