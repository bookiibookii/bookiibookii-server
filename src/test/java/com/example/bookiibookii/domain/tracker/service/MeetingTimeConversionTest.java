package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.GroupPlaceRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingTimeConversionTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private GroupsRepository groupsRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private GroupPlaceRepository groupPlaceRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private MeetingService meetingService;

    @Test
    void createMeetingConvertsOffsetRequestToUtcInstantResponse() {
        Groups group = directGroup(1L);
        MatchedMember host = member(1L, 10L, group, RoleStatus.HOST);
        MatchedMember guest = member(2L, 20L, group, RoleStatus.GUEST);
        MeetingRequestDTO request = request(OffsetDateTime.parse("2026-06-24T18:00:00+09:00"));

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = meetingService.createMeeting(group.getId(), request, host.getUser());

        assertThat(response.meetingAt()).isEqualTo(Instant.parse("2026-06-24T09:00:00Z"));
    }

    @Test
    void updateMeetingConvertsOffsetRequestToUtcInstantResponse() {
        Groups group = directGroup(2L);
        MatchedMember host = member(3L, 30L, group, RoleStatus.HOST);
        MatchedMember guest = member(4L, 40L, group, RoleStatus.GUEST);
        Meeting meeting = meeting(group, host, Instant.parse("2026-06-24T08:00:00Z"));
        MeetingRequestDTO request = request(OffsetDateTime.parse("2026-06-24T18:00:00+09:00"));

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        var response = meetingService.updateMeeting(group.getId(), request, host.getUser());

        assertThat(response.meetingAt()).isEqualTo(Instant.parse("2026-06-24T09:00:00Z"));
    }

    @Test
    void getMeetingReturnsStoredUtcInstant() {
        Groups group = directGroup(3L);
        MatchedMember host = member(5L, 50L, group, RoleStatus.HOST);
        Meeting meeting = meeting(group, host, Instant.parse("2026-06-24T09:00:00Z"));

        when(groupsRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), host.getUser().getId()))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        var response = meetingService.getMeeting(group.getId(), host.getUser());

        assertThat(response.meetingAt()).isEqualTo(Instant.parse("2026-06-24T09:00:00Z"));
    }

    private MeetingRequestDTO request(OffsetDateTime meetingAt) {
        return new MeetingRequestDTO(
                "카페",
                "서울시 강남구",
                "12345",
                BigDecimal.ONE,
                BigDecimal.ONE,
                "2층",
                meetingAt
        );
    }

    private Meeting meeting(Groups group, MatchedMember createdBy, Instant meetingAt) {
        return Meeting.builder()
                .id(1L)
                .group(group)
                .createdBy(createdBy)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .placeName("카페")
                .address("서울시 강남구")
                .zipCode("12345")
                .x(BigDecimal.ONE)
                .y(BigDecimal.ONE)
                .addressDetail("2층")
                .meetingAt(meetingAt)
                .build();
    }

    private Groups directGroup(Long id) {
        return Groups.builder()
                .id(id)
                .book(Book.builder().id(100L).title("교환 책").build())
                .tradeType(TradeType.DIRECT)
                .groupStatus(GroupStatus.MATCHED)
                .build();
    }

    private MatchedMember member(Long memberId, Long userId, Groups group, RoleStatus role) {
        return MatchedMember.builder()
                .id(memberId)
                .group(group)
                .user(User.builder().id(userId).nickName("사용자" + userId).build())
                .role(role)
                .readingStatus(ReadingStatus.EXCHANGING)
                .exchangeStatus(ExchangeStatus.MEETING_SCHEDULED)
                .build();
    }
}
