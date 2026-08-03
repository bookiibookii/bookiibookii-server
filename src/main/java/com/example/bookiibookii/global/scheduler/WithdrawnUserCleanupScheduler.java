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

        // 탈퇴 시 revoke 실패했던 Apple 유저 재시도
        // revoke 성공한 유저만 토큰을 null로 초기화해 삭제 대상에 포함
        // revoke 실패한 유저는 토큰이 남아있으므로 이번 삭제에서 제외되어 다음 실행 시 재시도
        List<User> appleUsers = userRepository.findWithdrawnAppleUsersForRevoke(deleteBefore);
        int successCount = 0;
        for (User user : appleUsers) {
            String token = user.getAppleRefreshToken();
            boolean revoked = appleAuthClient.revokeToken(token);
            if (revoked) {
                int updated = userRepository.clearAppleRefreshToken(user.getId(), token);
                if (updated > 0) {
                    successCount++;
                } else {
                    // revoke 성공했으나 DB 상태 불일치 — 재가입 등으로 상태 변경됨, 새 토큰 보존
                    log.warn("Apple token revoke 완료했으나 DB 초기화 스킵 (userId: {})", user.getId());
                }
            }
        }
        if (!appleUsers.isEmpty()) {
            log.info("Apple token revoke 재시도: {}건 시도, {}건 성공, {}건 다음 회차로 이월",
                    appleUsers.size(), successCount, appleUsers.size() - successCount);
        }

        int deleted = userRepository.deleteWithdrawnUsersBefore(deleteBefore);
        log.info("탈퇴 유저 하드삭제: {}건", deleted);
    }
}
