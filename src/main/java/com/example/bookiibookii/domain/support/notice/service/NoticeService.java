package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.entity.Notice;
import com.example.bookiibookii.domain.support.notice.entity.UserNoticeRead;
import com.example.bookiibookii.domain.support.notice.exception.NoticeException;
import com.example.bookiibookii.domain.support.notice.exception.code.NoticeErrorCode;
import com.example.bookiibookii.domain.support.notice.repository.NoticeRepository;
import com.example.bookiibookii.domain.support.notice.repository.UserNoticeReadRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.apiPayload.code.GeneralErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UserNoticeReadRepository userNoticeReadRepository;
    private final UserRepository userRepository;

    // 공지사항 리스트 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDTO.NoticeListDTO> getNoticeList(Long userId) {
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc();

        Set<Long> readNoticeIds = (userId != null && !notices.isEmpty())
                ? userNoticeReadRepository.findReadNoticeIds(userId, notices.stream().map(Notice::getId).toList())
                : Set.of();

        return notices.stream()
                .map(notice -> new NoticeResponseDTO.NoticeListDTO(
                        notice.getId(),
                        notice.getCreatedAt(),
                        notice.getTitle(),
                        notice.getSummary(),
                        readNoticeIds.contains(notice.getId())
                ))
                .toList();
    }

    // 공지사항 상세 조회
    @Transactional(readOnly = true)
    public NoticeResponseDTO.NoticeDetailDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        String authorNickname = null;
        if (notice.getUserId() != null) {
            authorNickname = userRepository.findById(notice.getUserId())
                    .map(User::getNickName)
                    .orElse(null);
        }

        return new NoticeResponseDTO.NoticeDetailDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getSummary(),
                notice.getContent(),
                authorNickname,
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    // 공지사항 읽음 처리
    public void markAsRead(Long userId, Long noticeId) {
        if (!noticeRepository.existsById(noticeId)) {
            throw new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND);
        }
        if (!userNoticeReadRepository.existsByUserIdAndNoticeId(userId, noticeId)) {
            userNoticeReadRepository.save(UserNoticeRead.builder()
                    .userId(userId)
                    .noticeId(noticeId)
                    .build());
        }
    }
}
