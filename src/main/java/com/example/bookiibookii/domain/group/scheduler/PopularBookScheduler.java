package com.example.bookiibookii.domain.group.scheduler;

import com.example.bookiibookii.domain.group.service.PopularBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

// 홈 인기 도서 TOP5는 요청마다 groups 전체를 집계하지 않고, 주기적으로 popular_book에 미리 집계해 둔다.
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularBookScheduler {

    private final PopularBookService popularBookService;

    // 기본 10분마다 실행. 환경별로 scheduler.popular-book.cron 설정으로 변경할 수 있다.
    @Scheduled(cron = "${scheduler.popular-book.cron:0 */10 * * * *}", zone = "Asia/Seoul")
    public void refreshPopularBooks() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            int count = popularBookService.refresh();
            stopWatch.stop();
            log.info("[Scheduler] 인기 도서 집계 완료: {}권, {}ms", count, stopWatch.getTotalTimeMillis());
        } catch (Exception e) {
            // 실패해도 기존 집계 결과는 유지된다 (트랜잭션 롤백)
            log.error("[Scheduler] 인기 도서 집계 실패 - 기존 데이터 유지", e);
        }
    }

    // 최초 배포 직후 테이블이 비어 있으면 첫 스케줄 전까지 홈 섹션이 비지 않도록 한 번 채운다.
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIfEmpty() {
        try {
            if (popularBookService.isEmpty()) {
                refreshPopularBooks();
            }
        } catch (Exception e) {
            log.error("[Scheduler] 인기 도서 초기 집계 실패", e);
        }
    }
}
