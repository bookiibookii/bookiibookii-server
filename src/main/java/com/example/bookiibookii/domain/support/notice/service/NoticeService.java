package com.example.bookiibookii.domain.support.notice.service;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.entity.Notice;
import com.example.bookiibookii.domain.support.notice.repository.NoticeRepository;
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
    @Transactional
    public NoticeResponseDTO.NoticeDetailDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        return new NoticeResponseDTO.NoticeDetailDTO(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getImage(),
                notice.getCreatedAt()
        );
    }
}
