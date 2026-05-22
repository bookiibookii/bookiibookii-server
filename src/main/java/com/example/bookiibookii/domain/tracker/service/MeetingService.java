package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.GroupPlace;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupPlaceSourceType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.GroupPlaceRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.exception.LocationException;
import com.example.bookiibookii.domain.location.exception.code.LocationErrorCode;
import com.example.bookiibookii.domain.location.repository.LocationRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingDefaultPlaceResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingResponseDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final LocationRepository locationRepository;
    private final GroupPlaceRepository groupPlaceRepository;

    @Transactional
    public MeetingResponseDTO createMeeting(Long groupId, MeetingRequestDTO request, User user) {
        Groups group = getDirectGroupForUpdate(groupId);
        List<MatchedMember> members = getMembersForUpdate(groupId);
        MatchedMember me = findMe(members, user.getId());
        validateHost(me);
        validateMeetingPhase(members);

        if (meetingRepository.existsByGroup_GroupId(groupId)) {
            throw new TrackerException(TrackerErrorCode.MEETING_ALREADY_EXISTS);
        }

        Location location = getLocation(request.locationId());
        Meeting meeting = Meeting.create(group, me, location, request.addressDetail(), request.scheduledAt());

        try {
            meeting = meetingRepository.save(meeting);
        } catch (DataIntegrityViolationException e) {
            throw new TrackerException(TrackerErrorCode.MEETING_ALREADY_EXISTS);
        }

        members.forEach(member -> member.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULED));

        return MeetingResponseDTO.from(meeting);
    }

    @Transactional
    public MeetingResponseDTO updateMeeting(Long groupId, MeetingRequestDTO request, User user) {
        getDirectGroupForUpdate(groupId);
        List<MatchedMember> members = getMembersForUpdate(groupId);
        Meeting meeting = meetingRepository.findByGroupIdForUpdate(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));
        MatchedMember me = findMe(members, user.getId());
        validateHost(me);
        validateMeetingPhase(members);

        Location location = getLocation(request.locationId());
        meeting.update(location, request.addressDetail(), request.scheduledAt());

        return MeetingResponseDTO.from(meeting);
    }

    public MeetingResponseDTO getMeeting(Long groupId, User user) {
        validateDirectGroup(groupId);
        MatchedMember me = validateGroupMember(groupId, user.getId());
        validateExchangePhase(me);
        return meetingRepository.findByGroupId(groupId)
                .map(MeetingResponseDTO::from)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));
    }

    public MeetingDefaultPlaceResponseDTO getDefaultPlace(Long groupId, User user) {
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (group.getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.NOT_DIRECT_TRADE_GROUP);
        }
        validateGroupMember(groupId, user.getId());

        GroupPlace groupPlace = groupPlaceRepository.findByGroup_GroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.GROUP_SELECTED_PLACE_NOT_FOUND));
        if (groupPlace.getSourceType() != GroupPlaceSourceType.USER_EXCHANGE) {
            throw new TrackerException(TrackerErrorCode.INVALID_GROUP_SELECTED_PLACE);
        }

        return MeetingDefaultPlaceResponseDTO.from(groupPlace);
    }

    @Transactional
    public MeetingResponseDTO completeMeeting(Long groupId, User user) {
        getDirectGroupForUpdate(groupId);
        Meeting meeting = meetingRepository.findByGroupIdForUpdate(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));
        List<MatchedMember> members = getMembersForUpdate(groupId);
        MatchedMember me = findMe(members, user.getId());
        validateMeetingPhase(members);

        if (me.getExchangeStatus() == ExchangeStatus.MEETING_COMPLETED) {
            throw new TrackerException(TrackerErrorCode.MEETING_ALREADY_COMPLETED);
        }
        if (me.getExchangeStatus() != ExchangeStatus.MEETING_SCHEDULED
                && me.getExchangeStatus() != ExchangeStatus.MEETING_FAILED) {
            throw new TrackerException(TrackerErrorCode.INVALID_MEETING_PHASE);
        }

        me.updateExchangeStatus(ExchangeStatus.MEETING_COMPLETED);

        if (members.stream().allMatch(member -> member.getExchangeStatus() == ExchangeStatus.MEETING_COMPLETED)) {
            completeMeetingPhase(members);
        }

        return MeetingResponseDTO.from(meeting);
    }

    private Groups getDirectGroupForUpdate(Long groupId) {
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (group.getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }
        return group;
    }

    private void validateDirectGroup(Long groupId) {
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (group.getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }
    }

    private List<MatchedMember> getMembersForUpdate(Long groupId) {
        List<MatchedMember> members = matchedMemberRepository.findAllByGroupIdForUpdate(groupId);
        if (members.size() != 2) {
            throw new TrackerException(TrackerErrorCode.INVALID_PARTNER_COUNT);
        }
        return members;
    }

    private MatchedMember findMe(List<MatchedMember> members, Long userId) {
        return members.stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }

    private MatchedMember validateGroupMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }

    private void validateHost(MatchedMember matchedMember) {
        if (matchedMember.getRole() != RoleStatus.HOST) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_HOST);
        }
    }

    private ReadingStatus validateMeetingPhase(List<MatchedMember> members) {
        boolean allExchanging = members.stream()
                .allMatch(member -> member.getReadingStatus() == ReadingStatus.EXCHANGING);
        if (allExchanging) {
            return ReadingStatus.EXCHANGING;
        }

        boolean allReturning = members.stream()
                .allMatch(member -> member.getReadingStatus() == ReadingStatus.RETURNING);
        if (allReturning) {
            return ReadingStatus.RETURNING;
        }

        throw new TrackerException(TrackerErrorCode.INVALID_MEETING_PHASE);
    }

    private void validateExchangePhase(MatchedMember matchedMember) {
        ReadingStatus readingStatus = matchedMember.getReadingStatus();
        if (readingStatus != ReadingStatus.EXCHANGING && readingStatus != ReadingStatus.RETURNING) {
            throw new TrackerException(TrackerErrorCode.INVALID_MEETING_PHASE);
        }
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new LocationException(LocationErrorCode.NOT_FOUND));
    }

    private void completeMeetingPhase(List<MatchedMember> members) {
        ReadingStatus phase = validateMeetingPhase(members);
        LocalDateTime now = LocalDateTime.now();

        if (phase == ReadingStatus.EXCHANGING) {
            members.forEach(member -> {
                member.changeCurrentMemberBook(findPartnerBook(member), now);
                member.updateReadingStatus(ReadingStatus.PARTNER_BOOK_READING);
                member.updateExchangeStatus(ExchangeStatus.NOT_STARTED);
            });
            return;
        }

        members.forEach(member -> member.updateExchangeStatus(ExchangeStatus.NOT_STARTED));
    }

    private MemberBook findPartnerBook(MatchedMember matchedMember) {
        return matchedMember.getMemberBooks().stream()
                .filter(memberBook -> !memberBook.isMine())
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.INVALID_CURRENT_MEMBER_BOOK));
    }
}
