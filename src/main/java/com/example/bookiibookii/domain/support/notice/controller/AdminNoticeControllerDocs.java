package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin Notice", description = "관리자용 공지 관련 API")
public interface AdminNoticeControllerDocs {
    @Operation(
            summary = "공지 등록 API",
            description = "공지글을 등록하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 등록 성공")
    })
    ApiResponse<Void> createNotice(
            @AuthenticationPrincipal(expression = "user") User user,
            NoticeRequestDTO.CreateNoticeDTO request);

}
