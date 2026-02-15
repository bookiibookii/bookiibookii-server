package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.req.NoticeRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

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

    @Operation(
            summary = "공지 수정 API",
            description = "공지글을 수정하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "NOTICE401_1", description = "공지글을 찾을 수 없습니다."),
    })
    ApiResponse<Void> updateNotice(
            @PathVariable Long noticeId,
            NoticeRequestDTO.UpdateNoticeDTO request);

    @Operation(
            summary = "공지 삭제 API",
            description = "공지글을 삭제하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "NOTICE401_1", description = "공지글을 찾을 수 없습니다."),
    })
    ApiResponse<Void> deleteNotice(@PathVariable Long noticeId);
}
