package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.util.ReadingPeriodDateCalculator;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerTopBannerResponse;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerTopBannerType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerTopBannerResolverTest {

    private static final ZoneId KST = ReadingPeriodDateCalculator.KST;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 11, 12, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();

    private final TrackerTopBannerResolver resolver = new TrackerTopBannerResolver(
            Clock.fixed(NOW.atZone(KST).toInstant(), KST)
    );

    @Test
    void readingEndingTodayReturnsReadingDDay() {
        TrackerTopBannerResponse banner = resolve(context(
                1L, "그룹", ReadingStatus.MY_BOOK_READING, TODAY
        ));

        assertThat(banner.bannerType()).isEqualTo(TrackerTopBannerType.READING_D_DAY);
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.bookTitle()).isEqualTo("현재 책");
        assertThat(banner.title()).isEqualTo("읽기 마감일이 오늘이에요.");
        assertThat(banner.titleTemplate()).isEqualTo("{bookTitle}을 오늘까지 읽어주세요.");
        assertThat(banner.dDayLabel()).isEqualTo("D-Day");
    }

    @Test
    void overdueReadingKeepsReadingDDay() {
        TrackerTopBannerResponse banner = resolve(context(
                1L, "그룹", ReadingStatus.PARTNER_BOOK_READING, TODAY.minusDays(2)
        ));

        assertThat(banner.bannerType()).isEqualTo(TrackerTopBannerType.READING_D_DAY);
    }

    @Test
    void futureReadingReturnsReadingInProgress() {
        TrackerTopBannerResponse banner = resolve(context(
                1L, "그룹", ReadingStatus.MY_BOOK_READING, TODAY.plusDays(1)
        ));

        assertThat(banner.bannerType()).isEqualTo(TrackerTopBannerType.READING_IN_PROGRESS);
        assertThat(banner.bookTitle()).isEqualTo("현재 책");
        assertThat(banner.title()).isEqualTo("독서 후기를 남겨주세요.");
        assertThat(banner.titleTemplate()).isEqualTo("{bookTitle}을 읽고 후기를 남겨주세요.");
    }

    @Test
    void futureDirectMeetingIncludesTargetAndRemainingSeconds() {
        Instant targetAt = instant(NOW.plus(java.time.Duration.ofHours(2)).plusMinutes(3).plusSeconds(4));
        TrackerTopBannerContext context = directContext(
                1L, "그룹", RoleStatus.HOST, ExchangeStatus.MEETING_SCHEDULED, targetAt
        );

        TrackerTopBannerResponse banner = resolve(context);

        assertThat(banner.bannerType()).isEqualTo(TrackerTopBannerType.DIRECT_MEETING_SCHEDULED);
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.bookTitle()).isNull();
        assertThat(banner.title())
                .isEqualTo("{nickname} 님과의 책 교환까지 {remainingTime} 남았어요.");
        assertThat(banner.title()).doesNotContain("예정되어 있어요");
        assertThat(banner.titleTemplate())
                .isEqualTo("{nickname} 님과의 책 교환까지 {remainingTime} 남았어요.");
        assertThat(banner.titleTemplate()).contains("{remainingTime}");
        assertThat(banner.title()).doesNotMatch(".*\\d{2,}:\\d{2}:\\d{2}.*");
        assertThat(banner.dDayLabel()).isEqualTo("D-Day");
        assertThat(banner.targetAt()).isEqualTo(targetAt);
        assertThat(banner.remainingSeconds()).isEqualTo(7384L);
    }

    @Test
    void overdueDirectMeetingUsesOverdueTitleAndZeroRemainingSeconds() {
        Instant targetAt = instant(NOW.minusSeconds(1));
        TrackerTopBannerContext context = directContext(
                1L, "그룹", RoleStatus.HOST, ExchangeStatus.MEETING_SCHEDULED, targetAt
        );

        TrackerTopBannerResponse banner = resolve(context);

        assertThat(banner.title()).isEqualTo("교환 약속 시간이 지났어요.");
        assertThat(banner.titleTemplate()).isNull();
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.dDayLabel()).isEqualTo("D-Day");
        assertThat(banner.targetAt()).isEqualTo(targetAt);
        assertThat(banner.remainingSeconds()).isNotNegative().isZero();
    }

    @Test
    void futureDirectMeetingIncludesCalendarDDayLabel() {
        Instant targetAt = instant(NOW.plusDays(6));

        TrackerTopBannerResponse banner = resolve(directContext(
                1L, "그룹", RoleStatus.HOST, ExchangeStatus.MEETING_SCHEDULED, targetAt
        ));

        assertThat(banner.dDayLabel()).isEqualTo("D-6");
    }

    @Test
    void directHostBeforeMeetingReturnsRegistrationRequired() {
        TrackerTopBannerResponse banner = resolve(directContext(
                1L, "그룹", RoleStatus.HOST, ExchangeStatus.MEETING_SCHEDULE_WAITING, null
        ));

        assertThat(banner.bannerType())
                .isEqualTo(TrackerTopBannerType.DIRECT_MEETING_REGISTER_REQUIRED_HOST);
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.titleTemplate())
                .isEqualTo("{nickname} 님과의 교환 약속을 등록해주세요.");
    }

    @Test
    void directGuestBeforeMeetingReturnsConfirmationRequired() {
        TrackerTopBannerResponse banner = resolve(directContext(
                1L, "그룹", RoleStatus.GUEST, ExchangeStatus.MEETING_SCHEDULE_WAITING, null
        ));

        assertThat(banner.bannerType())
                .isEqualTo(TrackerTopBannerType.DIRECT_MEETING_CONFIRM_REQUIRED_GUEST);
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.titleTemplate())
                .isEqualTo("{nickname} 님과의 교환 약속을 확인해주세요.");
    }

    @Test
    void deliveryTrackingWaitingReturnsRegistrationRequired() {
        TrackerTopBannerResponse banner = resolve(deliveryContext(
                1L, "그룹", ExchangeRound.FIRST_EXCHANGE, "내 책", "파트너 책"
        ));

        assertThat(banner.bannerType())
                .isEqualTo(TrackerTopBannerType.DELIVERY_TRACKING_REGISTER_REQUIRED);
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.bookTitle()).isEqualTo("내 책");
        assertThat(banner.title()).isEqualTo("발송이 필요해요.");
        assertThat(banner.titleTemplate())
                .isEqualTo("{nickname} 님께 {bookTitle}을 발송해주세요.");
    }

    @Test
    void deliveryBookTitleUsesBookActuallySentInEachRound() {
        TrackerTopBannerResponse first = resolve(deliveryContext(
                1L, "1차", ExchangeRound.FIRST_EXCHANGE, "내 책", "파트너 책"
        ));
        TrackerTopBannerResponse returned = resolve(deliveryContext(
                2L, "2차", ExchangeRound.RETURN_EXCHANGE, "내 책", "파트너 책"
        ));

        assertThat(first.bookTitle()).isEqualTo("내 책");
        assertThat(returned.bookTitle()).isEqualTo("파트너 책");
    }

    @Test
    void missingPartnerReviewReturnsExchangeReviewRequired() {
        TrackerTopBannerContext context = new TrackerTopBannerContext(
                1L, 10L, "그룹", "파트너", TradeType.DIRECT, RoleStatus.GUEST,
                ReadingStatus.PARTNER_REVIEWING, ExchangeStatus.NOT_STARTED, ExchangeStatus.NOT_STARTED,
                TODAY, "현재 책", null, null, null, false, null, null
        );

        TrackerTopBannerResponse banner = resolve(context);

        assertThat(banner.bannerType()).isEqualTo(TrackerTopBannerType.EXCHANGE_REVIEW_REQUIRED);
        assertThat(banner.partnerNickname()).isEqualTo("파트너");
        assertThat(banner.bookTitle()).isNull();
        assertThat(banner.titleTemplate())
                .isEqualTo("{nickname} 님과의 교환독서 후기를 남겨주세요.");
    }

    @Test
    void writtenExchangeReviewDoesNotReturnActionBannerWhileWaitingForPartner() {
        TrackerTopBannerContext context = new TrackerTopBannerContext(
                1L, 10L, "그룹", "파트너", TradeType.DIRECT, RoleStatus.HOST,
                ReadingStatus.PARTNER_REVIEWING, ExchangeStatus.NOT_STARTED, ExchangeStatus.NOT_STARTED,
                TODAY, "현재 책", null, null, null, true, null, null
        );

        assertThat(resolver.resolve(List.of(context))).isEmpty();
    }

    @Test
    void longBookTitleIsReturnedWithoutTruncation() {
        String longBookTitle = "아몬드 (양장 특별 한정판) - 제10회 창비 청소년문학상 수상작";
        TrackerTopBannerContext context = new TrackerTopBannerContext(
                1L, 10L, "그룹", "파트너", TradeType.DIRECT, RoleStatus.GUEST,
                ReadingStatus.MY_BOOK_READING, ExchangeStatus.NOT_STARTED, ExchangeStatus.NOT_STARTED,
                TODAY, longBookTitle, null, null, null, false, null, null
        );

        TrackerTopBannerResponse banner = resolve(context);

        assertThat(banner.bookTitle()).isEqualTo(longBookTitle);
        assertThat(banner.title()).doesNotContain(longBookTitle);
    }

    @Test
    void limitsCandidatesToTopThreePriorities() {
        List<TrackerTopBannerResponse> banners = resolver.resolve(List.of(
                exchangeReviewContext(4L, "라"),
                directContext(2L, "나", RoleStatus.HOST, ExchangeStatus.MEETING_SCHEDULED, instant(NOW.plus(java.time.Duration.ofHours(1)))),
                context(1L, "가", ReadingStatus.MY_BOOK_READING, TODAY),
                deliveryContext(3L, "다", ExchangeRound.FIRST_EXCHANGE, "내 책", "파트너 책")
        ));

        assertThat(banners).extracting(TrackerTopBannerResponse::bannerType)
                .containsExactly(
                        TrackerTopBannerType.READING_D_DAY,
                        TrackerTopBannerType.DIRECT_MEETING_SCHEDULED,
                        TrackerTopBannerType.DELIVERY_TRACKING_REGISTER_REQUIRED
                );
    }

    @Test
    void sortsSameBannerTypeByGroupNameThenGroupId() {
        List<TrackerTopBannerResponse> banners = resolver.resolve(List.of(
                context(3L, "나", ReadingStatus.MY_BOOK_READING, TODAY.plusDays(1)),
                context(2L, "가", ReadingStatus.MY_BOOK_READING, TODAY.plusDays(1)),
                context(1L, "가", ReadingStatus.MY_BOOK_READING, TODAY.plusDays(1))
        ));

        assertThat(banners).extracting(TrackerTopBannerResponse::groupId)
                .containsExactly(1L, 2L, 3L);
    }

    private TrackerTopBannerResponse resolve(TrackerTopBannerContext context) {
        return resolver.resolve(List.of(context)).get(0);
    }

    private TrackerTopBannerContext context(
            Long groupId,
            String groupName,
            ReadingStatus readingStatus,
            LocalDate readingEndDate
    ) {
        return new TrackerTopBannerContext(
                groupId, groupId * 10, groupName, "파트너", TradeType.DIRECT, RoleStatus.GUEST,
                readingStatus, ExchangeStatus.NOT_STARTED, ExchangeStatus.NOT_STARTED,
                readingEndDate, "현재 책", null, null, null, false, null, null
        );
    }

    private TrackerTopBannerContext directContext(
            Long groupId,
            String groupName,
            RoleStatus role,
            ExchangeStatus exchangeStatus,
            Instant scheduledAt
    ) {
        return new TrackerTopBannerContext(
                groupId, groupId * 10, groupName, "파트너", TradeType.DIRECT, role,
                ReadingStatus.EXCHANGING, exchangeStatus, ExchangeStatus.MEETING_SCHEDULED,
                TODAY, "내 책", ExchangeRound.FIRST_EXCHANGE, "내 책", null,
                false, scheduledAt, scheduledAt == null ? null : "약속 장소"
        );
    }

    private Instant instant(LocalDateTime dateTime) {
        return dateTime.atZone(KST).toInstant();
    }

    private TrackerTopBannerContext deliveryContext(
            Long groupId,
            String groupName,
            ExchangeRound exchangeRound,
            String firstBook,
            String returnBook
    ) {
        return new TrackerTopBannerContext(
                groupId, groupId * 10, groupName, "파트너", TradeType.DELIVERY, RoleStatus.GUEST,
                exchangeRound == ExchangeRound.FIRST_EXCHANGE
                        ? ReadingStatus.EXCHANGING
                        : ReadingStatus.RETURNING,
                ExchangeStatus.TRACKING_REGISTER_WAITING, ExchangeStatus.TRACKING_REGISTER_WAITING,
                TODAY, "현재 책", exchangeRound, firstBook, returnBook,
                false, null, null
        );
    }

    private TrackerTopBannerContext exchangeReviewContext(Long groupId, String groupName) {
        return new TrackerTopBannerContext(
                groupId, groupId * 10, groupName, "파트너", TradeType.DIRECT, RoleStatus.GUEST,
                ReadingStatus.PARTNER_REVIEWING, ExchangeStatus.NOT_STARTED, ExchangeStatus.NOT_STARTED,
                TODAY, "현재 책", null, null, null, false, null, null
        );
    }
}
