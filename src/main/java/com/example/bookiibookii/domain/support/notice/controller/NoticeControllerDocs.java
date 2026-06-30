package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Notice", description = "공지 관련 API")
public interface NoticeControllerDocs {

    @Operation(
            summary = "공지 리스트 조회 API",
            description = """
            공지 리스트를 조회합니다.
            - 로그인 상태: isRead = 해당 유저의 실제 읽음 여부
            - 비로그인 상태: isRead = false
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 리스트 조회 성공")
    })
    ApiResponse<List<NoticeResponseDTO.NoticeListDTO>> getNoticeList(
            @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : user") User user
    );

    @Operation(
            summary = "공지 상세 조회 API",
            description = """
            공지 상세 내용을 조회합니다.
            - 로그인 상태: 조회 시 자동으로 읽음 처리됩니다. 이미 읽은 공지는 중복 저장하지 않습니다.
            - 비로그인 상태: 읽음 처리 없이 내용만 반환합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지글을 찾을 수 없음")
    })
    ApiResponse<NoticeResponseDTO.NoticeDetailDTO> getNoticeDetail(
            @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : user") User user,
            @PathVariable Long noticeId
    );
}
