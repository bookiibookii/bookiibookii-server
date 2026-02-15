package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
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

@Service
@RequiredArgsConstructor
@Transactional
public class AdminNoticeService {
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;

    public void createNotice(Long userId, NoticeRequestDTO.CreateNoticeDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        Notice notice = Notice.builder()
                .userId(user.getId())
                .title(request.title())
                .content(request.content())
                .summary(request.summary())
                .image(request.image())
                .build();

        noticeRepository.save(notice);
    }

    public void updateNotice(Long noticeId, NoticeRequestDTO.UpdateNoticeDTO request) {
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
        if (request.image() != null) {
            notice.updateImage(request.image());
        }
    }
}
