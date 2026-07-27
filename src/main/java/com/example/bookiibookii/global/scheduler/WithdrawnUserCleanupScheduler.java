package com.example.bookiibookii.global.scheduler;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.auth.social.AppleAuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserCleanupScheduler {

    private final UserRepository userRepository;
    private final AppleAuthClient appleAuthClient;
    private final Clock clock;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredWithdrawnUsers() {
        Instant deleteBefore = clock.instant().minus(Duration.ofDays(30));

        // 탈퇴 시 revoke 실패했던 Apple 유저에 대해 재시도 (App Store 심사 지침)
        List<User> appleUsers = userRepository.findWithdrawnAppleUsersWithTokenBefore(deleteBefore);
        for (User user : appleUsers) {
            appleAuthClient.revokeToken(user.getAppleRefreshToken());
        }
        if (!appleUsers.isEmpty()) {
            log.info("Apple token revoke 재시도: {}건", appleUsers.size());
        }

        int deleted = userRepository.deleteWithdrawnUsersBefore(deleteBefore);
        log.info("탈퇴 유저 하드삭제: {}건", deleted);
    }
}
