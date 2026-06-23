package com.example.bookiibookii.domain.group.scheduler;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.service.GroupCompletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupScheduler {

    private final GroupsRepository groupsRepository;
    private final GroupCompletionService groupCompletionService;

    @Scheduled(cron = "0 0 3 * * *", zone="Asia/Seoul")
    public void forceCompleteGroups() {
        log.info("[Scheduler] 파트너 후기 미작성 그룹 강제 종료 프로세스 시작");

        LocalDateTime cutoff = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(14);
        List<Groups> timeoutGroups = groupsRepository.findGroupsForForceComplete(cutoff);

        for (Groups group : timeoutGroups) {
            try {
                // 3. 방금 만든 서비스 호출 (각 호출마다 독립적인 트랜잭션 생성)
                groupCompletionService.forceCompleteSingleGroup(group.getId());
                log.info("[Scheduler] 그룹 강제 종료 성공");
            } catch (Exception e) {
                // 4. 하나가 실패해도 catch문에서 잡히므로 다음 그룹 루프는 계속 돌아감!
                log.error("[Scheduler] 그룹 처리 중 오류 발생");
            }
        }

        log.info("[Scheduler] 리뷰 기간 만료 그룹 강제 종료 프로세스 완료 (처리 대상: {}건)", timeoutGroups.size());
    }
}