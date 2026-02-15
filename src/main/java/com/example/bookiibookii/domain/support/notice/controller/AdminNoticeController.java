package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
import com.example.bookiibookii.domain.support.notice.service.AdminNoticeService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notice")
@PreAuthorize("hasRole('ADMIN')") // 클래스 레벨에 선언하여 모든 메서드에 관리자 권한 강제
public class AdminNoticeController implements AdminNoticeControllerDocs {
    private final AdminNoticeService adminNoticeService;

    // 공지사항 등록
    @PostMapping
    public ApiResponse<Void> createNotice(
            @AuthenticationPrincipal(expression = "user") User user,
            NoticeRequestDTO.CreateNoticeDTO request) {
        adminNoticeService.createNotice(user.getId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }
}
