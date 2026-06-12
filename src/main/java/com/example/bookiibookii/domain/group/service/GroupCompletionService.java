package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forceCompleteSingleGroup(Long groupId) {
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (group.getGroupStatus() != GroupStatus.MATCHED) {
            log.info("MATCHED 상태가 아닌 그룹입니다. groupId={}", groupId);
            return;
        }

        List<MatchedMember> members = matchedMemberRepository.findAllByGroup_Id(groupId);
        boolean readyToComplete = members.size() == 2 && members.stream().allMatch(member ->
                member.getReadingStatus() == ReadingStatus.PARTNER_REVIEWING
                        && member.getExchangeStatus() == ExchangeStatus.NOT_STARTED
                        && member.isReviewWritten()
        );
        if (!readyToComplete) {
            log.info("최종 교환독서 후기 작성이 완료되지 않은 그룹입니다. groupId={}", groupId);
            return;
        }

        LocalDateTime completedAt = LocalDateTime.now();
        members.forEach(member -> member.completeReading(completedAt));
        group.updateStatus(GroupStatus.COMPLETED);
    }
}
