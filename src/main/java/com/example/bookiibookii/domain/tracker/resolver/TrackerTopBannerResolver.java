package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.util.ReadingPeriodDateCalculator;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerTopBannerResponse;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerTopBannerType;
import com.example.bookiibookii.global.time.TimeConfig;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class TrackerTopBannerResolver {

    private static final int MAX_BANNER_COUNT = 3;
    private static final String READING_D_DAY_TITLE_TEMPLATE =
            "{bookTitle}을 오늘까지 읽어주세요.";
    private static final String READING_IN_PROGRESS_TITLE_TEMPLATE =
            "{bookTitle}을 읽고 후기를 남겨주세요.";
    private static final String DELIVERY_TITLE_TEMPLATE =
            "{nickname} 님께 {bookTitle}을 발송해주세요.";
    private static final String DIRECT_MEETING_SCHEDULED_TITLE_TEMPLATE =
            "{nickname} 님과의 책 교환까지 {remainingTime} 남았어요.";
    private static final String DIRECT_MEETING_REGISTER_TITLE_TEMPLATE =
            "{nickname} 님과의 교환 약속을 등록해주세요.";
    private static final String DIRECT_MEETING_CONFIRM_TITLE_TEMPLATE =
            "{nickname} 님과의 교환 약속을 확인해주세요.";
    private static final String EXCHANGE_REVIEW_TITLE_TEMPLATE =
            "{nickname} 님과의 교환독서 후기를 남겨주세요.";
    private final Clock clock;

    public TrackerTopBannerResolver() {
        this(Clock.system(ReadingPeriodDateCalculator.KST));
    }

    public TrackerTopBannerResolver(Clock clock) {
        this.clock = clock;
    }

    public List<TrackerTopBannerResponse> resolve(List<TrackerTopBannerContext> contexts) {
        return contexts.stream()
                .map(this::resolve)
                .flatMap(Optional::stream)
                .sorted(Comparator
                        .comparingInt((TrackerTopBannerResponse banner) -> banner.bannerType().getPriority())
                        .thenComparing(
                                TrackerTopBannerResponse::groupName,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(
                                TrackerTopBannerResponse::groupId,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        ))
                .limit(MAX_BANNER_COUNT)
                .toList();
    }

    Optional<TrackerTopBannerResponse> resolve(TrackerTopBannerContext context) {
        LocalDate today = LocalDate.now(clock.withZone(TimeConfig.KST));
        Instant now = clock.instant();

        if (isReading(context.readingStatus()) && context.readingEndDate() != null
                && !context.readingEndDate().isAfter(today)) {
            // TODO: 독서 종료일이 지난 경우 별도 overdue 문구를 노출할지 PM 확인 후 수정
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.READING_D_DAY,
                    context.currentReadingBookTitle(),
                    "읽기 마감일이 오늘이에요.",
                    READING_D_DAY_TITLE_TEMPLATE,
                    "독서 후기도 꼭 남겨주세요.",
                    "D-Day",
                    null,
                    null
            ));
        }

        if (isDirectExchange(context) && context.meetingAt() != null
                && !isDirectExchangeCompleted(context)) {
            long remainingSeconds = Math.max(Duration.between(now, context.meetingAt()).getSeconds(), 0L);
            boolean meetingOverdue = !context.meetingAt().isAfter(now);
            // TODO: 약속 시간이 지난 뒤 미완료 상태에서 별도 CTA/상세 문구가 필요한지 PM 확인 후 수정
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.DIRECT_MEETING_SCHEDULED,
                    null,
                    meetingOverdue
                            ? "교환 약속 시간이 지났어요."
                            : DIRECT_MEETING_SCHEDULED_TITLE_TEMPLATE,
                    meetingOverdue ? null : DIRECT_MEETING_SCHEDULED_TITLE_TEMPLATE,
                    context.meetingPlaceName() + "에서 만나요.",
                    directMeetingDDayLabel(today, context.meetingAt().atZone(ReadingPeriodDateCalculator.KST).toLocalDate()),
                    context.meetingAt(),
                    remainingSeconds
            ));
        }

        if (isDeliveryTrackingRegistrationRequired(context)) {
            String bookTitle = resolveBookToSend(context);
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.DELIVERY_TRACKING_REGISTER_REQUIRED,
                    bookTitle,
                    "발송이 필요해요.",
                    DELIVERY_TITLE_TEMPLATE,
                    "책이 파손되지 않도록 꼼꼼히 포장해주세요.",
                    null,
                    null,
                    null
            ));
        }

        if (isReading(context.readingStatus()) && context.readingEndDate() != null
                && context.readingEndDate().isAfter(today)) {
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.READING_IN_PROGRESS,
                    context.currentReadingBookTitle(),
                    "독서 후기를 남겨주세요.",
                    READING_IN_PROGRESS_TITLE_TEMPLATE,
                    "독서카드를 작성하면 교환독서가 더 즐거워져요.",
                    null,
                    null,
                    null
            ));
        }

        if (isDirectMeetingRegistrationRequired(context) && context.myRole() == RoleStatus.HOST) {
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.DIRECT_MEETING_REGISTER_REQUIRED_HOST,
                    null,
                    "교환 약속 등록이 필요해요.",
                    DIRECT_MEETING_REGISTER_TITLE_TEMPLATE,
                    "파트너와 약속 장소 및 일시를 협의해주세요.",
                    null,
                    null,
                    null
            ));
        }

        if (isDirectMeetingRegistrationRequired(context) && context.myRole() == RoleStatus.GUEST) {
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.DIRECT_MEETING_CONFIRM_REQUIRED_GUEST,
                    null,
                    "교환 약속 확인이 필요해요.",
                    DIRECT_MEETING_CONFIRM_TITLE_TEMPLATE,
                    "파트너와 약속 장소 및 일시를 협의해주세요.",
                    null,
                    null,
                    null
            ));
        }

        if (context.readingStatus() == ReadingStatus.PARTNER_REVIEWING && !context.partnerReviewWritten()) {
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.EXCHANGE_REVIEW_REQUIRED,
                    null,
                    "교환독서 후기를 남겨주세요.",
                    EXCHANGE_REVIEW_TITLE_TEMPLATE,
                    "파트너와 함께 한 교환독서는 어떠셨나요?",
                    null,
                    null,
                    null
            ));
        }

        return Optional.empty();
    }

    private String directMeetingDDayLabel(LocalDate today, LocalDate meetingDate) {
        long remainingDays = Duration.between(
                today.atStartOfDay(),
                meetingDate.atStartOfDay()
        ).toDays();
        if (remainingDays <= 0) {
            return "D-Day";
        }
        return "D-" + remainingDays;
    }

    private TrackerTopBannerResponse banner(
            TrackerTopBannerContext context,
            TrackerTopBannerType bannerType,
            String bookTitle,
            String title,
            String titleTemplate,
            String subtitle,
            String dDayLabel,
            Instant targetAt,
            Long remainingSeconds
    ) {
        return TrackerTopBannerResponse.builder()
                .bannerType(bannerType)
                .groupId(context.groupId())
                .matchedMemberId(context.matchedMemberId())
                .groupName(context.groupName())
                .partnerNickname(context.partnerNickname())
                .bookTitle(bookTitle)
                .title(title)
                .titleTemplate(titleTemplate)
                .subtitle(subtitle)
                .dDayLabel(dDayLabel)
                .targetAt(targetAt)
                .remainingSeconds(remainingSeconds)
                .build();
    }

    private boolean isReading(ReadingStatus readingStatus) {
        return readingStatus == ReadingStatus.MY_BOOK_READING
                || readingStatus == ReadingStatus.PARTNER_BOOK_READING;
    }

    private boolean isDirectExchange(TrackerTopBannerContext context) {
        return context.tradeType() == TradeType.DIRECT && context.exchangeRound() != null;
    }

    private boolean isDirectExchangeCompleted(TrackerTopBannerContext context) {
        return context.myExchangeStatus() == ExchangeStatus.MEETING_COMPLETED
                && context.partnerExchangeStatus() == ExchangeStatus.MEETING_COMPLETED;
    }

    private boolean isDirectMeetingRegistrationRequired(TrackerTopBannerContext context) {
        if (!isDirectExchange(context) || context.meetingAt() != null) {
            return false;
        }
        return context.myExchangeStatus() == ExchangeStatus.MEETING_SCHEDULE_WAITING
                || context.myExchangeStatus() == ExchangeStatus.NOT_STARTED;
    }

    private boolean isDeliveryTrackingRegistrationRequired(TrackerTopBannerContext context) {
        return context.tradeType() == TradeType.DELIVERY
                && context.exchangeRound() != null
                && (context.myExchangeStatus() == ExchangeStatus.TRACKING_REGISTER_WAITING
                || context.myExchangeStatus() == ExchangeStatus.NOT_STARTED);
    }

    private String resolveBookToSend(TrackerTopBannerContext context) {
        return context.exchangeRound() == ExchangeRound.FIRST_EXCHANGE
                ? context.firstExchangeSendBookTitle()
                : context.returnExchangeSendBookTitle();
    }
}
