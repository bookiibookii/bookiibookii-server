package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Tag(name = "CardImage", description = "카드 이미지 관련 API (카드 수정 시 Presigned URL 발급)")
public interface CardImageControllerDocs {

    @Operation(
            summary = "Presigned URL 발급 (카드 이미지 수정용)",
            description = """
            기존 카드의 이미지를 수정하기 위한 presigned URL을 발급합니다.
            
            - UUID 기반 s3Key를 생성하여 presigned URL을 발급합니다.
            - **카드 수정 시 이미지 변경에 사용합니다.** 카드 생성 전 이미지 업로드는 `/api/card/{userBookId}/presigned-url`을 사용하세요.
            - 발급된 presignedPutUrl로 PUT 요청 후, 받은 s3Key를 **독서카드 수정 API** (`PATCH /api/card/{cardId}`)의 request body에 넣어 호출하세요.
            - URL은 10분간 유효합니다.
            - s3Key 형식: image/cards/{uuid}
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "URL 발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "카드를 찾을 수 없음"
            )
    })
    @PostMapping("/{cardId}/images/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrl(
            @Parameter(description = "카드 식별자(ID)", example = "1")
            @PathVariable Long cardId
    );
}
