package com.example.bookiibookii.domain.group.scheduler;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.service.GroupCompletionService;
import com.example.bookiibookii.global.notification.DiscordWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupScheduler {

    private static final String SCHEDULER_NAME = "그룹 강제 종료";

    private final GroupsRepository groupsRepository;
    private final GroupCompletionService groupCompletionService;
    private final DiscordWebhookService discordWebhookService;
    private final Clock clock;

    @Scheduled(cron = "0 0 3 * * *", zone="Asia/Seoul")
    public void forceCompleteGroups() {
        log.info("[Scheduler] 파트너 후기 미작성 그룹 강제 종료 프로세스 시작");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        int totalCount;
        int successCount = 0;
        int failCount = 0;

        try {
            Instant cutoff = clock.instant().minus(Duration.ofDays(14));
            List<Groups> timeoutGroups = groupsRepository.findGroupsForForceComplete(cutoff);
            totalCount = timeoutGroups.size();

            for (Groups group : timeoutGroups) {
                try {
                    // 3. 방금 만든 서비스 호출 (각 호출마다 독립적인 트랜잭션 생성)
                    groupCompletionService.forceCompleteSingleGroup(group.getId());
                    successCount++;
                    log.info("[Scheduler] 그룹 강제 종료 성공: groupId={}", group.getId());
                } catch (Exception e) {
                    // 4. 하나가 실패해도 catch문에서 잡히므로 다음 그룹 루프는 계속 돌아감!
                    failCount++;
                    log.error("[Scheduler] 그룹 처리 중 오류 발생: groupId={}", group.getId(), e);
                }
            }

            log.info("[Scheduler] 리뷰 기간 만료 그룹 강제 종료 프로세스 완료 (처리 대상: {}건)", totalCount);
        } catch (Exception e) {
            stopWatch.stop();
            log.error("[Scheduler] 그룹 강제 종료 프로세스 전체 실패", e);
            discordWebhookService.sendSchedulerError(SCHEDULER_NAME, e);
            return;
        }

        stopWatch.stop();
        discordWebhookService.sendSchedulerResult(
                SCHEDULER_NAME,
                totalCount,
                successCount,
                failCount,
                stopWatch.getTotalTimeMillis()
        );
    }
}
