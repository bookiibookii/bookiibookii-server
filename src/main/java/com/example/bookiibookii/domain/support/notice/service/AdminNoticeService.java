package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.entity.Notice;
import com.example.bookiibookii.domain.support.notice.exception.NoticeException;
import com.example.bookiibookii.domain.support.notice.exception.code.NoticeErrorCode;
import com.example.bookiibookii.domain.support.notice.repository.NoticeRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminNoticeService {
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<NoticeResponseDTO.AdminNoticeListDTO> getNoticeList() {
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc();

        Set<Long> userIds = notices.stream()
                .flatMap(n -> Stream.of(n.getUserId(), n.getUpdatedByUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> nicknameById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u.getNickName() != null ? u.getNickName() : ""));

        return notices.stream()
                .map(notice -> new NoticeResponseDTO.AdminNoticeListDTO(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getSummary(),
                        nicknameById.get(notice.getUserId()),
                        notice.getUpdatedByUserId() != null ? nicknameById.get(notice.getUpdatedByUserId()) : null,
                        notice.getCreatedAt(),
                        notice.getUpdatedAt()
                ))
                .toList();
    }

    public void createNotice(Long userId, NoticeRequestDTO.CreateNoticeDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        Notice notice = Notice.builder()
                .userId(user.getId())
                .title(request.title())
                .content(request.content())
                .summary(request.summary())
                .build();

        noticeRepository.save(notice);
    }

    @Transactional(readOnly = true)
    public NoticeResponseDTO.AdminNoticeDetailDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        User author = userRepository.findById(notice.getUserId())
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        String updatedByNickname = null;
        if (notice.getUpdatedByUserId() != null) {
            updatedByNickname = userRepository.findById(notice.getUpdatedByUserId())
                    .map(User::getNickName)
                    .orElse(null);
        }

        return new NoticeResponseDTO.AdminNoticeDetailDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getSummary(),
                notice.getContent(),
                author.getNickName(),
                updatedByNickname,
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    public void updateNotice(Long updatedByUserId, Long noticeId, NoticeRequestDTO.UpdateNoticeDTO request) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        if (request.title() != null) {
            notice.updateTitle(request.title());
        }
        if (request.content() != null) {
            notice.updateContent(request.content());
        }
        if (request.summary() != null) {
            notice.updateSummary(request.summary());
        }
        notice.updateUpdatedBy(updatedByUserId);
    }

    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeException(NoticeErrorCode.NOTICE_NOT_FOUND));

        noticeRepository.deleteById(noticeId);
    }
}
