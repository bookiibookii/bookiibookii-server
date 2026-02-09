package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.BookReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.req.GroupReviewRequestDTO;
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

    @PostMapping("/books/{userBookId}")
    public ApiResponse<Void> createBookReview(
            @PathVariable Long userBookId,
            @RequestBody @Valid BookReviewRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        reviewService.createBookReview(userBookId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_CREATED, null);
    }

    @PostMapping("/{groupId}/groupreview")
    public ApiResponse<Void> createGroupReview(
            @PathVariable Long groupId,
            @RequestBody @Valid GroupReviewRequestDTO.CreateGroupReviewDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        reviewService.createGroupReview(groupId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.GROUP_REVIEW_CREATED, null);
    }

    @GetMapping("/me/relay")
    public ApiResponse<GroupReviewResponseDTO.GroupReviewDetailDTO> getMyRelayReviews(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        // Service에서 가공된 DTO를 가져옵니다.
        GroupReviewResponseDTO.GroupReviewDetailDTO response = reviewService.getMyRelayReviewHistory(user);
        return ApiResponse.onSuccess(ReviewSuccessCode.RELAY_REVIEW_HISTORY_FETCHED, response);
    }
}
