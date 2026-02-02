package com.example.bookiibookii.global.scheduler;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
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
    private final GroupsRepository groupsRepository;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredWithdrawnUsers() {

        LocalDateTime deleteBefore = LocalDateTime.now().minusDays(30);
        userRepository.deleteWithdrawnUsersBefore(deleteBefore);

        // 30일보다 더 오래전에(<=) 삭제된 그룹들을 하드 삭제합니다.
        groupsRepository.deleteExpiredDeletedGroups(deleteBefore);
    }
}