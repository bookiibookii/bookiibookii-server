package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.exception.code.ReviewSuccessCode;
import com.example.bookiibookii.domain.review.service.ReviewService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/reviews")
@RequiredArgsConstructor
public class BookReviewController implements BookReviewControllerDocs {

    private final ReviewService reviewService;

    @PostMapping
    public ApiResponse<BookReviewResponseDTO> createBookReview(
            @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewUpsertDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        BookReviewResponseDTO response = reviewService.createBookReview(groupId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_CREATED, response);
    }

    @PatchMapping("/me")
    public ApiResponse<BookReviewResponseDTO> updateMyBookReview(
            @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewUpsertDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        BookReviewResponseDTO response = reviewService.updateMyBookReview(groupId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_UPDATED, response);
    }
}
