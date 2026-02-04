package com.example.bookiibookii.domain.group.scheduler;

import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupScheduler {

    private final GroupsRepository groupsRepository;
    private final ApplicationRepository applicationRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final DomainEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoProcessGroups() {
        LocalDate today = LocalDate.now();
        List<Groups> targetGroups = groupsRepository.findGroupsToStart(today);

        for (Groups group : targetGroups) {
            long memberCount = matchedMemberRepository.countByGroup(group);

            if (memberCount >= 2) {
                group.updateStatus(GroupStatus.MATCHED);
                //매칭 이벤트 발행
                eventPublisher.publish(new GroupMatchedEvent(
                        group.getGroupId(),
                        group.getHost().getId(),
                        group.getStartDate(),
                        group.getMaxCapacity()
                ));

            } else {
                group.markAsDELETED();
            }

            // 신청자 관리: 알림 발송을 위해 대기자 명단 먼저 추출
            List<Application> pendingApps = applicationRepository.findAllPendingByGroupId(group.getGroupId());

            for (Application app : pendingApps) {
                // 신청자들에게 '자동 거절' 알림 발송

            }

            // 어떤 경우든 남은 대기자(Pending)는 일괄 거절 처리
            applicationRepository.updatePendingToRejectedByGroupId(group.getGroupId(), ApplicationStatus.REJECTED);
        }
    }
}