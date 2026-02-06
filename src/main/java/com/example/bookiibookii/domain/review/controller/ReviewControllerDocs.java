package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.BookReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.req.GroupReviewRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Review", description = "책/그룹 리뷰 작성 API")
@RequestMapping("/api/reviews")
public interface ReviewControllerDocs {

    @Operation(
            summary = "책 리뷰 작성",
            description = """
            사용자의 UserBook에 평점(0.5 단위)과 코멘트를 저장합니다.
            
            - 경로: /api/reviews/books/{userBookId}
            - 조건: 해당 UserBook 소유자이며, 그룹 트래커가 COMPLETED 상태여야 합니다.
            - 코멘트는 최대 500자입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 평점/코멘트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음(소유자 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UserBook 또는 트래커 미존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "트래커 미완료 시 작성 불가")
    })
    @PostMapping("/books/{userBookId}")
    ApiResponse<Void> createBookReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid BookReviewRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "그룹 리뷰 작성 (상대 리뷰)",
            description = """
            1:1 교환독서 그룹에서 상대방에게 평점, 코멘트, 배지를 부여합니다.
            
            - 경로: /api/reviews/{groupId}/groupreview
            - 조건: 그룹 멤버이며 트래커가 COMPLETED 상태여야 합니다. 한 번만 작성 가능.
            - 코멘트는 최대 200자, 배지는 enum Badge 코드 배열로 전달합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 평점/코멘트 또는 중복 작성"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "매칭 정보 또는 트래커 미존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "트래커 미완료 시 작성 불가")
    })
    @PostMapping("/{groupId}/groupreview")
    ApiResponse<Void> createGroupReview(
            @PathVariable Long groupId,
            @RequestBody @Valid GroupReviewRequestDTO.CreateGroupReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    );
}
