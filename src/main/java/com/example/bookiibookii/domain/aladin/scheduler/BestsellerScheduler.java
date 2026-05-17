package com.example.bookiibookii.domain.aladin.scheduler;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.entity.BestsellerIsbn;
import com.example.bookiibookii.domain.aladin.repository.BestsellerIsbnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BestsellerScheduler {

    private static final int BESTSELLER_LIMIT = 10;

    private final AladinClient aladinClient;
    private final BestsellerIsbnRepository bestsellerIsbnRepository;

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void refreshBestsellers() {
        log.info("[Scheduler] 베스트셀러 갱신 시작");

        AladinClient.AladinItemSearchResponse response = aladinClient.fetchBestsellers(BESTSELLER_LIMIT);

        if (response == null || response.item() == null || response.item().isEmpty()) {
            log.warn("[Scheduler] 베스트셀러 응답 없음 — 기존 데이터 유지");
            return;
        }

        List<BestsellerIsbn> newList = new ArrayList<>();
        List<AladinClient.AladinBookItem> items = response.item();
        for (int i = 0; i < items.size(); i++) {
            String isbn13 = items.get(i).isbn13();
            if (isbn13 == null || isbn13.isBlank()) continue;
            newList.add(BestsellerIsbn.builder()
                    .isbn13(isbn13)
                    .rank(i + 1)
                    .build());
        }

        bestsellerIsbnRepository.deleteAll();
        bestsellerIsbnRepository.saveAll(newList);

        log.info("[Scheduler] 베스트셀러 갱신 완료 ({}건)", newList.size());
    }
}
