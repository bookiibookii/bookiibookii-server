package com.example.bookiibookii.domain.group.scheduler;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.service.GroupCompletionService;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;


import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.example.bookiibookii.domain.group.enums.GroupNotiType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupScheduler {

    private final GroupsRepository groupsRepository;
    private final ApplicationRepository applicationRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final DomainEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final GroupCompletionService groupCompletionService;

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
                // 매칭 성공 알람
//                eventPublisher.publish(new GroupNotificationEvent(
//                        MATCH_SUCCEEDED, group.getHost().getId(), group.getBook().getTitle(),
//                        newMember.getUser().getId(), null, group.getGroupId()
//                ));
                // 대기자들 거절 알람

            } else {
                group.markAsDELETED();
                eventPublisher.publish(new GroupNotificationEvent(MATCH_EXPIRED, null, group.getBook().getTitle(), group.getHost().getId(), null, group.getGroupId()));
                // 신청자 관리: 알림 발송을 위해 대기자 명단 먼저 추출
                List<Long> rcvIds = applicationRepository.findPendingUserIdsByGroupId(group.getGroupId());
                eventPublisher.publish(new GroupNotificationEvent(MATCH_AUTO_REJECTED, group.getHost().getId(), group.getBook().getTitle(), null, rcvIds, group.getGroupId()));
            }

            // 어떤 경우든 남은 대기자(Pending)는 일괄 거절 처리
            applicationRepository.updatePendingToRejectedByGroupId(group.getGroupId(), ApplicationStatus.REJECTED);
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void forceCompleteGroups() {
        log.info("[Scheduler] 리뷰 기간 만료 그룹 강제 종료 프로세스 시작");

        LocalDate deadline = LocalDate.now().minusDays(3);
        List<Groups> timeoutGroups = groupsRepository.findGroupsPastReviewDeadline(deadline);

        for (Groups group : timeoutGroups) {
            try {
                // 3. 방금 만든 서비스 호출 (각 호출마다 독립적인 트랜잭션 생성)
                groupCompletionService.forceCompleteSingleGroup(group.getGroupId());
                log.info("[Scheduler] 그룹 강제 종료 성공");
            } catch (Exception e) {
                // 4. 하나가 실패해도 catch문에서 잡히므로 다음 그룹 루프는 계속 돌아감!
                log.error("[Scheduler] 그룹 처리 중 오류 발생");
            }
        }

        log.info("[Scheduler] 리뷰 기간 만료 그룹 강제 종료 프로세스 완료 (처리 대상: {}건)", timeoutGroups.size());
    }
}