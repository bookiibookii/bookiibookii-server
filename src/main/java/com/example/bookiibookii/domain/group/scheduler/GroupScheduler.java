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

    private static final String SCHEDULER_NAME = "GroupForceComplete";

    private final GroupsRepository groupsRepository;
    private final GroupCompletionService groupCompletionService;
    private final DiscordWebhookService discordWebhookService;
    private final Clock clock;

    @Scheduled(cron = "0 0 3 * * *", zone="Asia/Seoul")
    public void forceCompleteGroups() {
        log.info("[Scheduler] 파트너 후기 미작성 그룹 강제 종료 프로세스 시작");

        StopWatch sw = new StopWatch();
        sw.start();
        int success = 0, fail = 0;

        try {
            Instant cutoff = clock.instant().minus(Duration.ofDays(14));
            List<Groups> timeoutGroups = groupsRepository.findGroupsForForceComplete(cutoff);

            for (Groups group : timeoutGroups) {
                try {
                    groupCompletionService.forceCompleteSingleGroup(group.getId());
                    success++;
                } catch (Exception e) {
                    fail++;
                    log.error("[Scheduler] 그룹 처리 실패 groupId={}", group.getId(), e);
                }
            }

            sw.stop();
            log.info("[Scheduler] 완료 (처리 대상: {}건 | 성공: {}건 | 실패: {}건 | {}ms)",
                    timeoutGroups.size(), success, fail, sw.getTotalTimeMillis());
            discordWebhookService.sendSchedulerResult(SCHEDULER_NAME, timeoutGroups.size(), success, fail, sw.getTotalTimeMillis());

        } catch (Exception e) {
            log.error("[Scheduler] 전체 실패", e);
            discordWebhookService.sendSchedulerError(SCHEDULER_NAME, e);
        }
    }
}
