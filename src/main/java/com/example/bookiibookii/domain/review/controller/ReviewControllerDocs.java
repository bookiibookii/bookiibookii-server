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
            1:N 함께 읽기 그룹 종료 후 사용자의 GroupBook에 책에 대한 평점과 코멘트를 저장합니다.
            
            - 경로: /api/reviews/together/{groupBookId}
            - 조건: 해당 GroupBook 소유자여야 합니다.
            - 코멘트: 최대 500자
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 평점/코멘트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(소유자 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GroupBook 미존재")
    })
    @PostMapping("/together/{groupBookId}")
    ApiResponse<Void> createTogetherReview(
            @PathVariable Long groupBookId,
            @RequestBody @Valid ReviewRequestDTO.TogetherReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "릴레이 중간 책 리뷰 작성 (1차/2차 독서 완료 후)",
            description = """
            1차 또는 2차 독서를 완료한 후 책에 대한 리뷰를 작성합니다.
            양측 모두 완료 시 자동으로 교환/반납 단계로 진입합니다.

            - 경로: /api/reviews/relay/{groupBookId}/book
            - 조건: 트래커가 READING 또는 READING_2 상태이며 독서 완료(READ_DONE/READ_DONE_2) 상태여야 합니다.
            """
    )
    @PostMapping("/relay/{groupBookId}/book")
    ApiResponse<Void> createMidRelayBookReview(
            @PathVariable Long groupBookId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "릴레이 통합 리뷰 작성 (책 + 파트너, 릴레이 종료 후)",
            description = """
            1:1 이어읽기 그룹 종료 후 책에 대한 리뷰와 상대방에 대한 평가를 한 번에 작성합니다.
            
            - 경로: /api/reviews/relay/{groupBookId}
            - 조건: 그룹 멤버이며 트래커가 RETURNED 상태여야 합니다. 한 번만 작성 가능.
            - 데이터: 책 평점/코멘트(500자), 파트너 평점/코멘트(200자) 전달
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 평점/코멘트 또는 중복 작성"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한없음/소유자아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GroupBook 또는 트래커 미존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "트래커 미반납(RETURNED) 시 작성 불가")
    })
    @PostMapping("/relay/{groupBookId}")
    ApiResponse<Void> createRelayReview(
            @PathVariable Long groupBookId,
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