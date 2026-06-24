package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewsResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MyBookReviewsResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MyGroupReviewsResponseDTO;
import com.example.bookiibookii.domain.review.exception.code.ReviewSuccessCode;
import com.example.bookiibookii.domain.review.service.ReviewService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public ApiResponse<GroupReviewsResponseDTO> getGroupReviews(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        GroupReviewsResponseDTO response = reviewService.getGroupReviews(groupId, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.GROUP_REVIEWS_FOUND, response);
    }

    @GetMapping("/book/me")
    public ApiResponse<MyBookReviewsResponseDTO> getMyBookReviews(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        MyBookReviewsResponseDTO response = reviewService.getMyBookReviews(groupId, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.MY_BOOK_REVIEWS_FOUND, response);
    }

    @PostMapping
    public ApiResponse<BookReviewResponseDTO> createBookReview(
            @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewUpsertDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        BookReviewResponseDTO response = reviewService.createBookReview(groupId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_CREATED, response);
    }

    @PatchMapping("/book/{reviewId}")
    public ApiResponse<BookReviewResponseDTO> updateMyBookReview(
            @PathVariable Long groupId,
            @PathVariable Long reviewId,
            @RequestBody @Valid ReviewRequestDTO.BookReviewUpsertDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        BookReviewResponseDTO response = reviewService.updateMyBookReview(groupId, reviewId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.BOOK_REVIEW_UPDATED, response);
    }

    @PatchMapping("/my-group")
    public ApiResponse<MyGroupReviewsResponseDTO> updateMyGroupReviews(
            @PathVariable Long groupId,
            @RequestBody @Valid ReviewRequestDTO.MyGroupReviewsUpdateDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        MyGroupReviewsResponseDTO response = reviewService.updateMyGroupReviews(groupId, request, user);
        return ApiResponse.onSuccess(ReviewSuccessCode.MY_GROUP_REVIEWS_UPDATED, response);
    }
}
