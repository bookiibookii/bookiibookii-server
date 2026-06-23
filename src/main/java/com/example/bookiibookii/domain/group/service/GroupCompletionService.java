package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupCompletionService {

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forceCompleteSingleGroup(Long groupId) {
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (group.getGroupStatus() != GroupStatus.MATCHED) {
            log.info("MATCHED 상태가 아닌 그룹입니다. groupId={}", groupId);
            return;
        }

        List<MatchedMember> members = matchedMemberRepository.findAllByGroup_Id(groupId);
        boolean readyToForceComplete = members.size() == 2 && members.stream().allMatch(member ->
                member.getReadingStatus() == ReadingStatus.PARTNER_REVIEWING
                        && member.getExchangeStatus() == ExchangeStatus.NOT_STARTED
        );
        if (!readyToForceComplete) {
            log.info("강제 종료 조건 미충족 그룹입니다. groupId={}", groupId);
            return;
        }

        LocalDateTime completedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        members.forEach(member -> member.completeReading(completedAt));
        group.updateStatus(GroupStatus.COMPLETED);

        List<Long> receiverIds = members.stream().map(mm -> mm.getUser().getId()).toList();
        eventPublisher.publish(new TrackerNotificationEvent(
                NotificationType.TRACKER_GROUP_FORCE_COMPLETED,
                null, null, null,
                receiverIds,
                groupId,
                ExchangeType.from(group.getTradeType()),
                null, null, null, null, null, null
        ));
    }
}
