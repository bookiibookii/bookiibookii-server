package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewResponseDTO;
import com.example.bookiibookii.domain.review.exception.code.ReviewSuccessCode;
import com.example.bookiibookii.domain.review.service.ReviewService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController implements ReviewControllerDocs {

    private final ReviewService reviewService;

    /**
     * 1. [함께 읽기] 전용 리뷰 생성
     * 파트너 리뷰 없이 책에 대한 평점/리뷰만 남깁니다.
     */
    @PostMapping("/together/{userBookId}")
    public ApiResponse<Void> createTogetherReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid ReviewRequestDTO.TogetherReviewDTO request, // 🟢 신규 DTO 사용
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        reviewService.createTogetherReview(userBookId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_CREATED, null);
    }

    /**
     * 2. [릴레이 중간] 책 리뷰 생성 (1차/2차 독서 완료 후)
     */
    @PostMapping("/relay/{userBookId}/book")
    public ApiResponse<Void> createMidRelayBookReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        reviewService.createMidRelayBookReview(userBookId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_CREATED, null);
    }

    /**
     * 3. [릴레이] 통합 리뷰 생성 (릴레이 종료 후, 파트너 리뷰 포함)
     */
    @PostMapping("/relay/{userBookId}")
    public ApiResponse<Void> createRelayReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid ReviewRequestDTO.RelayReviewDTO request, // 🟢 통합 DTO 사용
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        reviewService.createRelayReview(userBookId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.GROUP_REVIEW_CREATED, null);
    }

    /**
     * 3. 내 릴레이 리뷰 기록 조회
     */
    @GetMapping("/me/relay")
    public ApiResponse<GroupReviewResponseDTO.GroupReviewDetailDTO> getMyRelayReviews(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        GroupReviewResponseDTO.GroupReviewDetailDTO response = reviewService.getMyRelayReviewHistory(user);
        return ApiResponse.onSuccess(ReviewSuccessCode.RELAY_REVIEW_HISTORY_FETCHED, response);
    }
}