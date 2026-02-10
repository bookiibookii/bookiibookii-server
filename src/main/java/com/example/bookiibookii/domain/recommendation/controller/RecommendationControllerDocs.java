package com.example.bookiibookii.domain.recommendation.controller;

import com.example.bookiibookii.domain.recommendation.dto.res.RecommendationResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Recommendation", description = "추천 기능 관련 API")
public interface RecommendationControllerDocs {
    // api/recommendations/bookmates
    @Operation(
            summary = "부키메이트 추천 API",
            description = """
            상위 UserTag가 일치하면서, 그룹을 운영 중인 타 사용자를 최대 5명 추천합니다.
            - 각 항목에 userId, nickname, profileImageUrl(프로필 이미지 Presigned GET URL, 미등록 시 null), matchedTags, recentBookTitle이 포함됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "부키메이트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USERTAG404_1", description = "사용자의 태그가 존재하지 않습니다.")
    })
    ApiResponse<List<RecommendationResponseDTO.BookmateDto>> recommendBookmates(@AuthenticationPrincipal User user);

    // api/recommendations/groups
    @Operation(
            summary = "그룹 추천 API",
            description = """
            태그 일치도가 높은 그룹 3개를 추천합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "추천 그룹 조회 성공")
    })
    ApiResponse<List<RecommendationResponseDTO.RecommendedGroupDto>> recommendGroups(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "refresh", defaultValue = "false") boolean isRefresh);
}
