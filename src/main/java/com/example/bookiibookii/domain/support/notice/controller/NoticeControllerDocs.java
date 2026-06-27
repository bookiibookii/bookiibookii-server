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
            description = "공지 상세 내용을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지글을 찾을 수 없음")
    })
    ApiResponse<NoticeResponseDTO.NoticeDetailDTO> getNoticeDetail(@PathVariable Long noticeId);

    @Operation(
            summary = "공지 읽음 처리 API",
            description = """
            공지 상세 진입 시 호출합니다.
            - 이미 읽은 공지는 중복 저장하지 않습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지글을 찾을 수 없음")
    })
    ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long noticeId
    );
}
