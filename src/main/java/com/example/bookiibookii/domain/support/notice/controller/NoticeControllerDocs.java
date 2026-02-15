package com.example.bookiibookii.domain.support.notice.controller;

import com.example.bookiibookii.domain.support.notice.dto.res.NoticeResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Notice", description = "공지 관련 API")
public interface NoticeControllerDocs {
    @Operation(
            summary = "공지 리스트 조회 API",
            description = "공지 리스트를 조회하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 리스트 조회 성공")
    })
    ApiResponse<List<NoticeResponseDTO.NoticeListDTO>> getNoticeList();

    @Operation(
            summary = "공지 상세 조회 API",
            description = "공지 상세 내용을 조회하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "공지 상세 조회 성공")
    })
    ApiResponse<NoticeResponseDTO.NoticeDetailDTO> getNoticeDetail(@PathVariable Long noticeId);
}
