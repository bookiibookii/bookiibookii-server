package com.example.bookiibookii.domain.aladin.scheduler;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.entity.BestsellerIsbn;
import com.example.bookiibookii.domain.aladin.repository.BestsellerIsbnRepository;
import com.example.bookiibookii.domain.book.service.BookCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestsellerScheduler {

    private static final int BESTSELLER_LIMIT = 10;

    private final AladinClient aladinClient;
    private final BestsellerIsbnRepository bestsellerIsbnRepository;
    private final BookCategoryMapper bookCategoryMapper;

    // 매일 00:00 KST 실행. 환경별로 scheduler.bestseller.cron 설정으로 변경할 수 있다.
    @Scheduled(cron = "${scheduler.bestseller.cron:0 0 0 * * *}", zone = "Asia/Seoul")
    @Transactional
    public int refreshBestsellers() {
        log.info("[Scheduler] 베스트셀러 갱신 시작");

        AladinClient.AladinItemSearchResponse response;
        try {
            response = aladinClient.fetchBestsellers(BESTSELLER_LIMIT);
        } catch (RuntimeException e) {
            log.error("[Scheduler] 알라딘 베스트셀러 조회 실패 - 기존 데이터 유지", e);
            throw e;
        }

        if (response == null || response.item() == null || response.item().isEmpty()) {
            log.warn("[Scheduler] 베스트셀러 응답 없음 — 기존 데이터 유지");
            return 0;
        }

        List<BestsellerIsbn> newList = new ArrayList<>();
        Set<String> seenIsbn13 = new LinkedHashSet<>();
        List<AladinClient.AladinBookItem> items = response.item();
        for (int i = 0; i < items.size(); i++) {
            AladinClient.AladinBookItem item = items.get(i);
            if (item == null) {
                continue;
            }
            String isbn13 = normalizeIsbn13(item.isbn13());
            if (isbn13 == null) {
                continue;
            }

            if (bookCategoryMapper.mapCategory(
                    item.categoryId(),
                    item.categoryName(),
                    isbn13,
                    item.title()
            ).isEmpty()) {
                log.debug(
                        "[Scheduler] 차단 카테고리 베스트셀러 제외 categoryId={}, categoryName={}, isbn={}, title={}",
                        item.categoryId(),
                        item.categoryName(),
                        isbn13,
                        item.title()
                );
                continue;
            }

            if (isBlank(item.title()) || isBlank(item.author()) || isBlank(item.cover())) {
                log.warn("[Scheduler] 베스트셀러 표시 정보 누락으로 제외 isbn13={}", isbn13);
                continue;
            }

            if (!seenIsbn13.add(isbn13)) {
                continue;
            }
            newList.add(BestsellerIsbn.builder()
                    .isbn13(isbn13)
                    .rank(i + 1)
                    .title(item.title())
                    .author(item.author())
                    .bookImage(item.cover())
                    .build());
        }

        if (newList.isEmpty()) {
            log.warn("[Scheduler] 저장 가능한 베스트셀러가 없어 기존 데이터 유지");
            return 0;
        }

        bestsellerIsbnRepository.deleteAllInBatch();
        bestsellerIsbnRepository.saveAllAndFlush(newList);

        log.info("[Scheduler] 베스트셀러 갱신 완료 ({}건)", newList.size());
        return newList.size();
    }

    private String normalizeIsbn13(String isbn13) {
        if (isbn13 == null) {
            return null;
        }
        String normalized = isbn13.replaceAll("\\D", "");
        return normalized.length() == 13 ? normalized : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
