package com.example.bookiibookii.domain.group.scheduler;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.service.GroupCompletionService;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final GroupCompletionService groupCompletionService;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void autoProcessGroups() {
        log.info("[Scheduler] firedAtKST={}", ZonedDateTime.now(ZoneId.of("Asia/Seoul")));
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        List<Groups> targetGroups = groupsRepository.findGroupsToStart(today);

        for (Groups group : targetGroups) {
            long memberCount = matchedMemberRepository.countByGroup(group);
            GroupStatus oldStatus = group.getGroupStatus();

            // 통합 로직 호출: 오늘 날짜와 인원수로 상태 판단
            group.syncStatus(memberCount, today);

            // 1. 매칭 성공(MATCHED)으로 변한 경우
            if (group.getGroupStatus() == GroupStatus.MATCHED && oldStatus != GroupStatus.MATCHED) {
                // 매칭 이벤트 발행 (트래커 생성 등 후속 조치용)
                eventPublisher.publish(new GroupMatchedEvent(
                        group.getGroupId(),
                        group.getHost().getId(),
                        group.getStartDate(),
                        group.getMaxCapacity()
                ));
                // 매칭 성공 알림 발행
                List<Long> memberIds = matchedMemberRepository.findUserIdsByGroupId(group.getGroupId());

                eventPublisher.publish(new GroupNotificationEvent(
                        MATCH_SUCCEEDED, group.getHost().getId(), group.getBook().getTitle(),
                        null, memberIds, group.getGroupId()
                ));
            }

            // 2. 그룹삭제(DELETED)로 변한 경우
            else if (group.getGroupStatus() == GroupStatus.DELETED && oldStatus != GroupStatus.DELETED) {
                // 호스트에게 매칭 실패(기간 만료) 알림 발송
                eventPublisher.publish(new GroupNotificationEvent(
                        MATCH_EXPIRED, group.getHost().getId(), group.getBook().getTitle(),
                        group.getHost().getId(), null, group.getGroupId()
                ));
            }

            // 3. 상태가 RECRUITING이 아니게 되었다면 (매칭됐든 폭파됐든), 대기자들 일괄 거절 처리
            if (group.getGroupStatus() != GroupStatus.RECRUITING) {
                List<Long> pendingUserIds = applicationRepository.findPendingUserIdsByGroupId(group.getGroupId());

                if (!pendingUserIds.isEmpty()) {
                    // 대기자들에게 자동 거절 알림 발송
                    eventPublisher.publish(new GroupNotificationEvent(
                            MATCH_AUTO_REJECTED, group.getHost().getId(), group.getBook().getTitle(),
                            null, pendingUserIds, group.getGroupId()
                    ));

                    // DB 상태 일괄 업데이트
                    applicationRepository.updatePendingToRejectedByGroupId(group.getGroupId(), ApplicationStatus.REJECTED);
                }
            }
        }
    }


    @Scheduled(cron = "0 0 3 * * *", zone="Asia/Seoul")
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