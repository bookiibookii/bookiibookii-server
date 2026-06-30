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

        // PENDING 신청 일괄 거절
        List<Long> pendingUserIds = applicationRepository.findPendingUserIdsByGroupId(groupId);
        if (!pendingUserIds.isEmpty()) {
            applicationRepository.updatePendingToRejectedByGroupId(groupId, ApplicationStatus.REJECTED);
        }

        if (currentStatus == GroupStatus.RECRUITING) {
            // 호스트 + 신청자 모두에게 알림
            List<Long> receiverIds = new ArrayList<>(pendingUserIds);
            receiverIds.add(group.getHost().getId());
            publisher.publish(new GroupNotificationEvent(
                    GROUP_FORCE_CLOSED, null, title, null, receiverIds, groupId, null, null
            ));
        } else {
            // MATCHED: MatchedMember 완료 처리 후 멤버에게 알림
            Instant now = clock.instant();
            List<MatchedMember> members = matchedMemberRepository.findAllByGroup_Id(groupId);
            members.forEach(member -> member.completeReading(now));

            List<Long> memberIds = members.stream().map(mm -> mm.getUser().getId()).toList();
            publisher.publish(new GroupNotificationEvent(
                    GROUP_FORCE_CLOSED, null, title, null, memberIds, groupId, null,
                    ExchangeType.from(group.getTradeType())
            ));
        }

        group.markAsDELETED();
    }

    private String groupTitle(Groups group) {
        if (group.getGroupName() != null && !group.getGroupName().isBlank()) {
            return group.getGroupName();
        }
        return group.getBook().getTitle();
    }
}
