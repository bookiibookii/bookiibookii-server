package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.entity.UserNoticeRead;
import com.example.bookiibookii.domain.support.notice.repository.UserNoticeReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserNoticeReadService {

    private final UserNoticeReadRepository userNoticeReadRepository;

    // REQUIRES_NEW로 독립 트랜잭션 실행 — 중복 저장 실패 시 외부 트랜잭션에 영향 없음
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsRead(Long userId, Long noticeId) {
        try {
            userNoticeReadRepository.save(UserNoticeRead.builder()
                    .userId(userId)
                    .noticeId(noticeId)
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // 이미 읽은 공지 — 정상 케이스
        }
    }
}
