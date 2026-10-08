package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupCompletionServiceTest {

    @Mock
    private GroupsRepository groupsRepository;

    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private GroupCompletionService groupCompletionService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-06-20T05:00:00Z"));
    }

    @Test
    void completesGroupWhenForceCompletionConditionIsMetEvenIfFinalReviewsAreMissing() {
        Groups group = Groups.builder()
                .id(1L)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(1L, 10L, group);
        MatchedMember partner = member(2L, 20L, group);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroup_Id(group.getId()))
                .thenReturn(List.of(me, partner));

        groupCompletionService.forceCompleteSingleGroup(group.getId());

        assertThat(group.getGroupStatus()).isEqualTo(GroupStatus.COMPLETED);
        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(partner.getReadingStatus()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(me.getCompletedAt()).isEqualTo(Instant.parse("2026-06-20T05:00:00Z"));
        assertThat(partner.getCompletedAt()).isEqualTo(me.getCompletedAt());
        assertThat(me.isReviewWritten()).isFalse();
        assertThat(partner.isReviewWritten()).isFalse();
    }

    @Test
    void completesGroupWhenBothPartnerReviewsAreWritten() {
        Groups group = Groups.builder()
                .id(2L)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(3L, 30L, group);
        MatchedMember partner = member(4L, 40L, group);
        me.markReviewAsWritten();
        partner.markReviewAsWritten();

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroup_Id(group.getId()))
                .thenReturn(List.of(me, partner));

        groupCompletionService.forceCompleteSingleGroup(group.getId());

        assertThat(group.getGroupStatus()).isEqualTo(GroupStatus.COMPLETED);
        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(partner.getReadingStatus()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(me.getCompletedAt()).isNotNull();
        assertThat(partner.getCompletedAt()).isEqualTo(me.getCompletedAt());
    }

    private MatchedMember member(Long memberId, Long userId, Groups group) {
        return MatchedMember.builder()
                .id(memberId)
                .group(group)
                .user(User.builder().id(userId).build())
                .readingStatus(ReadingStatus.PARTNER_REVIEWING)
                .exchangeStatus(ExchangeStatus.NOT_STARTED)
                .build();
    }
}
