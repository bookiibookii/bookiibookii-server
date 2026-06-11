package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.util.ReadingPeriodDateCalculator;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerTopBannerResponse;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerTopBannerType;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class TrackerTopBannerResolver {

    private static final int MAX_BANNER_COUNT = 3;
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
        LocalDate today = LocalDate.now(clock);
        LocalDateTime now = LocalDateTime.now(clock);

        if (isReading(context.readingStatus()) && context.readingEndDate() != null
                && !context.readingEndDate().isAfter(today)) {
            // TODO: 독서 종료일이 지난 경우 별도 overdue 문구를 노출할지 PM 확인 후 수정
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.READING_D_DAY,
                    readingDDayTitle(context.currentReadingBookTitle()),
                    "독서 후기도 꼭 남겨주세요.",
                    "D-Day",
                    null,
                    null
            ));
        }

        if (isDirectExchange(context) && context.meetingScheduledAt() != null
                && !isDirectExchangeCompleted(context)) {
            long remainingSeconds = Math.max(Duration.between(now, context.meetingScheduledAt()).getSeconds(), 0L);
            // TODO: 약속 시간이 지난 뒤 미완료 상태에서 별도 CTA/상세 문구가 필요한지 PM 확인 후 수정
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.DIRECT_MEETING_SCHEDULED,
                    directMeetingTitle(context.partnerNickname(), context.meetingScheduledAt(), now),
                    context.meetingPlaceName() + "에서 만나요.",
                    null,
                    context.meetingScheduledAt(),
                    remainingSeconds
            ));
        }

        if (isDeliveryTrackingRegistrationRequired(context)) {
            return Optional.of(banner(
                    context,
                    TrackerTopBannerType.DELIVERY_TRACKING_REGISTER_REQUIRED,
                    deliveryTitle(context.partnerNickname(), resolveBookToSend(context)),
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
                    readingInProgressTitle(context.currentReadingBookTitle()),
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
                    directMeetingRegisterTitle(context.partnerNickname()),
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
                    directMeetingConfirmTitle(context.partnerNickname()),
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
                    exchangeReviewTitle(context.partnerNickname()),
                    "파트너와 함께 한 교환독서는 어떠셨나요?",
                    null,
                    null,
                    null
            ));
        }

        return Optional.empty();
    }

    String readingDDayTitle(String bookTitle) {
        return bookTitle + "을 오늘까지 읽어주세요.";
    }

    String readingInProgressTitle(String bookTitle) {
        return bookTitle + "을 읽고 후기를 남겨주세요.";
    }

    String directMeetingTitle(String nickname, LocalDateTime targetAt, LocalDateTime now) {
        if (!targetAt.isAfter(now)) {
            return "교환 약속 시간이 지났어요.";
        }
        long remainingSeconds = Duration.between(now, targetAt).getSeconds();
        return nickname + " 님과의 책 교환까지 "
                + formatRemainingTime(remainingSeconds)
                + " 남았어요.";
    }

    String deliveryTitle(String nickname, String bookTitle) {
        return nickname + " 님께 " + bookTitle + "을 발송해주세요.";
    }

    String directMeetingRegisterTitle(String nickname) {
        return nickname + " 님과의 교환 약속을 등록해주세요.";
    }

    String directMeetingConfirmTitle(String nickname) {
        return nickname + " 님과의 교환 약속을 확인해주세요.";
    }

    String exchangeReviewTitle(String nickname) {
        return nickname + " 님과의 교환독서 후기를 남겨주세요.";
    }

    String formatRemainingTime(long remainingSeconds) {
        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        long seconds = remainingSeconds % 60;
        return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
    }

    private TrackerTopBannerResponse banner(
            TrackerTopBannerContext context,
            TrackerTopBannerType bannerType,
            String title,
            String subtitle,
            String dDayLabel,
            LocalDateTime targetAt,
            Long remainingSeconds
    ) {
        return TrackerTopBannerResponse.builder()
                .bannerType(bannerType)
                .groupId(context.groupId())
                .matchedMemberId(context.matchedMemberId())
                .groupName(context.groupName())
                .title(title)
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
        if (!isDirectExchange(context) || context.meetingScheduledAt() != null) {
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
