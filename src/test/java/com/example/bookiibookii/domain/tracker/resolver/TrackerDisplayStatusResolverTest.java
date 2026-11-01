package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerDisplayStatusResolverTest {

    private final TrackerDisplayStatusResolver resolver = new TrackerDisplayStatusResolver();

    @Test
    void hostMustRegisterMeetingBeforeDirectExchange() {
        assertThat(resolveDirect(
                RoleStatus.HOST,
                ExchangeStatus.MEETING_SCHEDULE_WAITING,
                ExchangeStatus.MEETING_SCHEDULE_WAITING
        )).isEqualTo(TrackerDisplayStatus.MEETING_REGISTER_REQUIRED);
    }

    @Test
    void guestWaitsForHostMeetingRegistration() {
        assertThat(resolveDirect(
                RoleStatus.GUEST,
                ExchangeStatus.MEETING_SCHEDULE_WAITING,
                ExchangeStatus.MEETING_SCHEDULE_WAITING
        )).isEqualTo(TrackerDisplayStatus.WAITING_HOST_MEETING_REGISTER);
    }

    @Test
    void waitsForPartnerWhenMyMeetingCompletionIsDone() {
        assertThat(resolveDirect(
                RoleStatus.HOST,
                ExchangeStatus.MEETING_COMPLETED,
                ExchangeStatus.MEETING_SCHEDULED
        )).isEqualTo(TrackerDisplayStatus.WAITING_PARTNER_MEETING_COMPLETE);
    }

    @Test
    void waitsForPartnerReceiptConfirmationWhenMineIsDone() {
        assertThat(resolver.resolve(
                ReadingStatus.EXCHANGING,
                ExchangeStatus.RECEIVED_CONFIRMED,
                ExchangeStatus.TRACKING_REGISTERED,
                TradeType.DELIVERY,
                RoleStatus.GUEST,
                false
        )).isEqualTo(TrackerDisplayStatus.WAITING_PARTNER_RECEIPT_CONFIRM);
    }

    @Test
    void waitsForPartnerTrackingRegistrationWhenMineIsDone() {
        assertThat(resolver.resolve(
                ReadingStatus.EXCHANGING,
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTER_WAITING,
                TradeType.DELIVERY,
                RoleStatus.GUEST,
                false
        )).isEqualTo(TrackerDisplayStatus.WAITING_PARTNER_TRACKING_REGISTER);
    }

    @Test
    void partnerReviewingIsShownAsExchangeReviewWriting() {
        assertThat(resolver.resolve(
                ReadingStatus.PARTNER_REVIEWING,
                ExchangeStatus.NOT_STARTED,
                ExchangeStatus.NOT_STARTED,
                TradeType.DIRECT,
                RoleStatus.GUEST,
                false
        )).isEqualTo(TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING);
    }

    @Test
    void partnerReviewingWaitsForPartnerAfterMyExchangeReviewIsWritten() {
        assertThat(resolver.resolve(
                ReadingStatus.PARTNER_REVIEWING,
                ExchangeStatus.NOT_STARTED,
                ExchangeStatus.NOT_STARTED,
                TradeType.DIRECT,
                RoleStatus.HOST,
                false,
                true
        )).isEqualTo(TrackerDisplayStatus.EXCHANGE_REVIEW_WAITING_PARTNER);
    }

    @Test
    void completedReadingIsShownAsCompleted() {
        assertThat(resolver.resolve(
                ReadingStatus.COMPLETED,
                ExchangeStatus.NOT_STARTED,
                ExchangeStatus.NOT_STARTED,
                TradeType.DELIVERY,
                RoleStatus.GUEST,
                false,
                true
        )).isEqualTo(TrackerDisplayStatus.COMPLETED);
    }

    @Test
    void bookReviewStateDependsOnlyOnCurrentBookReview() {
        assertThat(resolver.resolve(
                ReadingStatus.PARTNER_BOOK_REVIEWING,
                ExchangeStatus.NOT_STARTED,
                ExchangeStatus.NOT_STARTED,
                TradeType.DELIVERY,
                RoleStatus.GUEST,
                true
        )).isEqualTo(TrackerDisplayStatus.REVIEW_WAITING_PARTNER);
    }

    private TrackerDisplayStatus resolveDirect(
            RoleStatus role,
            ExchangeStatus mine,
            ExchangeStatus partner
    ) {
        return resolver.resolve(
                ReadingStatus.EXCHANGING,
                mine,
                partner,
                TradeType.DIRECT,
                role,
                false
        );
    }
}
