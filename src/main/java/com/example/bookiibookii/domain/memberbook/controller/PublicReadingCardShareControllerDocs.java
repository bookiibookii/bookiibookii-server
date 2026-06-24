package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.memberbook.dto.res.PublicReadingCardResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "PublicReadingCard", description = "독서카드 공유 공개 API")
@RequestMapping("/api/public/reading-cards")
public interface PublicReadingCardShareControllerDocs {

    @GetMapping("/{shareToken}")
    @Operation(
            summary = "공유 토큰 기반 독서카드 공개 조회",
            description = """
            웹 공유 페이지에서 고유 링크(token)로 독서카드를 조회합니다.

            - 비로그인 공개 API입니다.
            - `shareLayout`(OVERLAY/SPLIT)과 카드 데이터를 반환하며, 웹은 이 값으로 공유 레이아웃을 분기합니다.
            - userId, groupId, 북마크, 리액션 등 내부/개인정보는 포함하지 않습니다.
            - revoked/expired 토큰, 삭제된 카드, 공유 불가 상태 카드는 404로 처리됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PublicReadingCardResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 링크 없음 또는 만료")
    })
    ApiResponse<PublicReadingCardResponseDTO> getPublicReadingCard(
            @Parameter(description = "공유 토큰(UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable String shareToken
    );
}
