package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.res.PublicProfileResponseDTO;
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

@Tag(name = "PublicProfile", description = "프로필 공유 공개 API")
@RequestMapping("/api/public/profiles")
public interface PublicProfileShareControllerDocs {

    @GetMapping("/{shareToken}")
    @Operation(
            summary = "공유 토큰 기반 프로필 공개 조회",
            description = """
            웹 공유 페이지에서 고유 링크(token)로 프로필을 조회합니다.

            - 비로그인 공개 API입니다.
            - userId, 성별, 생년월일, 후기 등 내부/개인정보는 포함하지 않습니다.
            - revoked 토큰, 탈퇴한 사용자 프로필은 404로 처리됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PublicProfileResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공유 링크 없음 또는 만료")
    })
    ApiResponse<PublicProfileResponseDTO> getPublicProfile(
            @Parameter(description = "공유 토큰(UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable String shareToken
    );
}
