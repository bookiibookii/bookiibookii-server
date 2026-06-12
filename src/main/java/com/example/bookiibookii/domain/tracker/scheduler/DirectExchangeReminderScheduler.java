package com.example.bookiibookii.domain.tracker.scheduler;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.resolver.ActiveExchangeRoundResolver;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DirectExchangeReminderScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final MeetingRepository meetingRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final DomainEventPublisher eventPublisher;
    private final ActiveExchangeRoundResolver activeExchangeRoundResolver;

    @Scheduled(
            cron = "${scheduler.direct-exchange-reminder.cron:0 */5 * * * *}",
            zone = "Asia/Seoul"
    )
    @Transactional(readOnly = true)
    public void sendOverdueMeetingReminders() {
        LocalDateTime cutoff = LocalDateTime.now(KST).minusHours(1);
        List<Meeting> meetings = meetingRepository.findDueDirectMeetingReminders(
                cutoff,
                TradeType.DIRECT,
                GroupStatus.MATCHED,
                ExchangeStatus.MEETING_SCHEDULED,
                ExchangeRound.FIRST_EXCHANGE,
                ReadingStatus.EXCHANGING,
                ExchangeRound.RETURN_EXCHANGE,
                ReadingStatus.RETURNING
        );

        for (Meeting meeting : meetings) {
            List<MatchedMember> members = matchedMemberRepository.findAllByGroup_Id(meeting.getGroup().getId());
            if (activeExchangeRoundResolver.resolve(members)
                    .filter(meeting.getExchangeRound()::equals)
                    .isEmpty()) {
                continue;
            }
            members.stream()
                    .filter(member -> member.getExchangeStatus() == ExchangeStatus.MEETING_SCHEDULED)
                    .forEach(member -> publishReminder(meeting, member));
        }
    }

    private void publishReminder(Meeting meeting, MatchedMember receiver) {
        eventPublisher.publish(new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_CONFIRM_REMINDER,
                null,
                null,
                receiver.getUser().getId(),
                meeting.getGroup().getId(),
                meeting.getExchangeRound(),
                meeting.getScheduledAt(),
                null,
                meeting.getGroup().getBook().getTitle()
        ));
    }
}
