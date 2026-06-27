package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.entity.Notice;
import com.example.bookiibookii.domain.support.notice.repository.NoticeRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.apiPayload.code.GeneralErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;

    // 공지사항 리스트 조회
    public List<NoticeResponseDTO.NoticeListDTO> getNoticeList() {
        return noticeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(notice -> new NoticeResponseDTO.NoticeListDTO(
                        notice.getId(),
                        notice.getCreatedAt(),
                        notice.getTitle(),
                        notice.getSummary()
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
}
