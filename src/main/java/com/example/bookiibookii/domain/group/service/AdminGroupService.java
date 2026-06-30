package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.example.bookiibookii.domain.group.enums.GroupNotiType.GROUP_FORCE_CLOSED;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminGroupService {

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final ApplicationRepository applicationRepository;
    private final DomainEventPublisher publisher;
    private final Clock clock;

    public void forceCloseGroup(Long groupId) {
        Groups group = groupsRepository.findByIdForUpdateAllStatuses(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        GroupStatus currentStatus = group.getGroupStatus();
        if (currentStatus == GroupStatus.COMPLETED || currentStatus == GroupStatus.DELETED) {
            throw new GroupException(GroupErrorCode.GROUP_ALREADY_CLOSED);
        }

        String title = groupTitle(group);
        Long hostId = group.getHost().getId();
        ExchangeType exchangeType = ExchangeType.from(group.getTradeType());

        // MATCHED: bulk update 전에 MatchedMember 완료 처리
        List<Long> memberIds = List.of();
        if (currentStatus == GroupStatus.MATCHED) {
            Instant now = clock.instant();
            List<MatchedMember> members = matchedMemberRepository.findAllByGroup_Id(groupId);
            members.forEach(member -> member.completeReading(now));
            memberIds = members.stream().map(mm -> mm.getUser().getId()).toList();
        }

        // 그룹 상태 변경 후 flush — bulk update(clearAutomatically=true)로 컨텍스트가
        // 초기화되기 전에 DB에 반영해 detached 문제 방지
        group.markAsDELETED();
        groupsRepository.flush();

        // PENDING 신청 일괄 거절 (flush 이후 실행)
        List<Long> pendingUserIds = applicationRepository.findPendingUserIdsByGroupId(groupId);
        if (!pendingUserIds.isEmpty()) {
            applicationRepository.updatePendingToRejectedByGroupId(groupId, ApplicationStatus.REJECTED);
        }

        // 알림 발송
        if (currentStatus == GroupStatus.RECRUITING) {
            List<Long> receiverIds = new ArrayList<>(pendingUserIds);
            receiverIds.add(hostId);
            publisher.publish(new GroupNotificationEvent(
                    GROUP_FORCE_CLOSED, null, title, null, receiverIds, groupId, null, null
            ));
        } else {
            publisher.publish(new GroupNotificationEvent(
                    GROUP_FORCE_CLOSED, null, title, null, memberIds, groupId, null, exchangeType
            ));
        }
    }

    private String groupTitle(Groups group) {
        if (group.getGroupName() != null && !group.getGroupName().isBlank()) {
            return group.getGroupName();
        }
        return group.getBook().getTitle();
    }
}
