package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.support.notice.service.AdminNoticeService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notice")
@PreAuthorize("hasRole('ADMIN')") // 클래스 레벨에 선언하여 모든 메서드에 관리자 권한 강제
public class AdminNoticeController implements AdminNoticeControllerDocs {
    private final AdminNoticeService adminNoticeService;

    // 공지사항 전체 목록 조회
    @GetMapping
    public ApiResponse<List<NoticeResponseDTO.AdminNoticeListDTO>> getNoticeList() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, adminNoticeService.getNoticeList());
    }

    // 공지사항 등록
    @PostMapping
    public ApiResponse<Void> createNotice(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody NoticeRequestDTO.CreateNoticeDTO request) {
        adminNoticeService.createNotice(user.getId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    // 공지사항 수정
    @PatchMapping("/{noticeId}")
    public ApiResponse<Void> updateNotice(
            @PathVariable Long noticeId,
            @RequestBody NoticeRequestDTO.UpdateNoticeDTO request
    ) {
        adminNoticeService.updateNotice(noticeId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    // 공지사항 삭제
    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        adminNoticeService.deleteNotice(noticeId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

}
