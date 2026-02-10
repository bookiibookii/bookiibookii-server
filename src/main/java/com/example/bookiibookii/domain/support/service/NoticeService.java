package com.example.bookiibookii.domain.support.service;

import com.example.bookiibookii.domain.support.converter.SupportConverter;
import com.example.bookiibookii.domain.support.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.entity.Notice;
import com.example.bookiibookii.domain.support.repository.NoticeRepository;
import com.example.bookiibookii.global.apiPayload.code.GeneralErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final SupportConverter supportConverter;

    // 공지사항 리스트 조회
    public List<NoticeResponseDTO.NoticeListDTO> getNoticeList() {
        List<Notice> notices = noticeRepository.findAllByOrderByCreatedAtDesc();
        return supportConverter.toNoticeListDTO(notices);
    }

    // 공지사항 상세 조회
    public NoticeResponseDTO.NoticeDetailDTO getNoticeDetail(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.NOT_FOUND));

        return supportConverter.toNoticeDetailDTO(notice);
    }
}
