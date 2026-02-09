package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO; // 🟢 통합 DTO 임포트
import com.example.bookiibookii.domain.review.dto.res.GroupReviewResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Review", description = "책/그룹 리뷰 작성 API")
@RequestMapping("/api/reviews")
public interface ReviewControllerDocs {

    @Operation(
            summary = "함께 읽기 리뷰 작성",
            description = """
            1:N 함께 읽기 그룹 종료 후 사용자의 UserBook에 책에 대한 평점과 코멘트를 저장합니다.
            
            - 경로: /api/reviews/together/{userBookId}
            - 조건: 해당 UserBook 소유자여야 합니다.
            - 코멘트: 최대 500자
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 평점/코멘트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(소유자 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UserBook 미존재")
    })
    @PostMapping("/together/{userBookId}")
    ApiResponse<Void> createTogetherReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid ReviewRequestDTO.TogetherReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "릴레이 통합 리뷰 작성 (책 + 파트너)",
            description = """
            1:1 이어읽기 그룹 종료 후 책에 대한 리뷰와 상대방에 대한 평가를 한 번에 작성합니다.
            
            - 경로: /api/reviews/relay/{userBookId}
            - 조건: 그룹 멤버이며 트래커가 RETURNED 상태여야 합니다. 한 번만 작성 가능.
            - 데이터: 책 평점/코멘트(500자), 파트너 평점/코멘트(200자), 배지 리스트 전달
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 평점/코멘트 또는 중복 작성"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한없음/소유자아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UserBook 또는 트래커 미존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "트래커 미반납(RETURNED) 시 작성 불가")
    })
    @PostMapping("/relay/{userBookId}")
    ApiResponse<Void> createRelayReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid ReviewRequestDTO.RelayReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "내 이어읽기 리뷰 히스토리 조회",
            description = """
            사용자가 참여하여 종료된 이어읽기(RELAY) 그룹의 평가와 파트너의 독후감을 조회합니다.
            
            - 경로: /api/reviews/me/relay
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/me/relay")
    ApiResponse<GroupReviewResponseDTO.GroupReviewDetailDTO> getMyRelayReviews(
            @AuthenticationPrincipal(expression = "user") User user
    );
}