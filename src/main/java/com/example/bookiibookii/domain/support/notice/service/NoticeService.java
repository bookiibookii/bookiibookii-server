package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.entity.Notice;
import com.example.bookiibookii.domain.support.notice.repository.NoticeRepository;
import com.example.bookiibookii.domain.support.notice.repository.UserNoticeReadRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.global.apiPayload.code.GeneralErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final NoticeRepository noticeRepository;
    private final UserNoticeReadRepository userNoticeReadRepository;
    private final UserNoticeReadService userNoticeReadService;
    private final UserRepository userRepository;
    private final UserImageS3Service userImageS3Service;

    // 공지사항 리스트 조회
    @Transactional(readOnly = true)
    public List<NoticeResponseDTO.NoticeListDTO> getNoticeList(Long userId) {
        List<Notice> notices = noticeRepository.findAllByOrderByUpdatedAtDesc();
        if (notices.isEmpty()) return List.of();

        Set<Long> readNoticeIds = userId != null
                ? userNoticeReadRepository.findReadNoticeIds(userId, notices.stream().map(Notice::getId).toList())
                : Set.of();

        List<Long> authorIds = notices.stream()
                .map(Notice::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> userById = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return notices.stream()
                .map(notice -> {
                    User author = notice.getUserId() != null ? userById.get(notice.getUserId()) : null;
                    String authorNickname = author != null ? author.getNickName() : null;
                    String authorProfileImageUrl = resolveProfileImageUrl(author);
                    return new NoticeResponseDTO.NoticeListDTO(
                            notice.getId(),
                            notice.getUpdatedAt(),
                            notice.getTitle(),
                            notice.getSummary(),
                            readNoticeIds.contains(notice.getId()),
                            authorNickname,
                            authorProfileImageUrl
                    );
                })
                .toList();
    }

    // 공지사항 상세 조회 (로그인 상태면 읽음 처리 병행)
    public NoticeResponseDTO.NoticeDetailDTO getNoticeDetail(Long noticeId, Long userId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        if (userId != null) {
            userNoticeReadService.markAsRead(userId, noticeId);
        }

        User author = notice.getUserId() != null
                ? userRepository.findById(notice.getUserId()).orElse(null)
                : null;

        return new NoticeResponseDTO.NoticeDetailDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getSummary(),
                notice.getContent(),
                author != null ? author.getNickName() : null,
                resolveProfileImageUrl(author),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private String resolveProfileImageUrl(User user) {
        if (user == null || user.getUserImage() == null) return null;
        return userImageS3Service.generatePresignedGetUrl(
                user.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
    }
}
