package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
import com.example.bookiibookii.domain.support.notice.entity.Notice;
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
}
