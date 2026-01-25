package com.example.bookiibookii.global.scheduler;

import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserCleanupScheduler {

    private final UserRepository userRepository;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredWithdrawnUsers() {

        LocalDateTime deleteBefore = LocalDateTime.now().minusDays(30);
        userRepository.deleteWithdrawnUsersBefore(deleteBefore);
    }
}