package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.service.NoticeService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notice")
public class NoticeController implements NoticeControllerDocs {
    private final NoticeService noticeService;

    // 공지사항 전체 목록 조회
    @GetMapping
    public ApiResponse<List<NoticeResponseDTO.NoticeListDTO>> getNoticeList(
            @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : user") User user
    ) {
        Long userId = user != null ? user.getId() : null;
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, noticeService.getNoticeList(userId));
    }

    // 공지사항 상세 조회
    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeResponseDTO.NoticeDetailDTO> getNoticeDetail(
            @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : user") User user,
            @PathVariable Long noticeId
    ) {
        Long userId = user != null ? user.getId() : null;
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, noticeService.getNoticeDetail(noticeId, userId));
    }
}
