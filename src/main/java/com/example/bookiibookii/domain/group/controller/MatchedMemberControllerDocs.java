package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.dto.res.MatchedMemberResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Group", description = "그룹 관련 API")
public interface MatchedMemberControllerDocs {

    @Operation(
            summary = "함께읽기 완독 처리 API",
            description = "사용자가 '다 읽었어요' 버튼을 클릭했을 때 독서율을 100%로 변경하고 완독 시간을 기록합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "완독 처리 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "MEMBER404_1: 해당 그룹의 멤버를 찾을 수 없음 (또는 그룹 없음)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "MEMBER400_1: 이미 완독 처리가 완료된 상태임",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "FORBIDDEN_GROUP_ACCESS: 해당 그룹에 접근 권한이 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<MatchedMemberResponseDTO.CompleteReadingResultDTO> completeTogetherReading(
            @Parameter(description = "그룹 ID", example = "1") @PathVariable(name = "groupId") Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
