package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.service.NoticeService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController implements NoticeControllerDocs{
    private final NoticeService noticeService;

    // 공지사항 전체 목록 조회
    @GetMapping
    public ApiResponse<List<NoticeResponseDTO.NoticeListDTO>> getNoticeList() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, noticeService.getNoticeList());
    }

    // 공지사항 상세 조회
    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeResponseDTO.NoticeDetailDTO> getNoticeDetail(@PathVariable Long noticeId) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, noticeService.getNoticeDetail(noticeId));
    }
}
