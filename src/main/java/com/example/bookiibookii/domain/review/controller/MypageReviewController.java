package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.res.MypageReviewResponseDTO;
import com.example.bookiibookii.domain.review.exception.code.ReviewSuccessCode;
import com.example.bookiibookii.domain.review.service.MypageReviewService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage/reviews")
@RequiredArgsConstructor
public class MypageReviewController implements MypageReviewControllerDocs {

    private final MypageReviewService mypageReviewService;

    @Override
    @GetMapping("/written")
    public ApiResponse<MypageReviewResponseDTO.WrittenReviews> getWrittenReviews(
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.onSuccess(
                ReviewSuccessCode.MYPAGE_WRITTEN_REVIEWS_FOUND,
                mypageReviewService.getWrittenReviews(user.getId(), pageable)
        );
    }

    @Override
    @GetMapping("/received")
    public ApiResponse<MypageReviewResponseDTO.ReceivedReviews> getReceivedReviews(
            @AuthenticationPrincipal(expression = "user") User user,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.onSuccess(
                ReviewSuccessCode.MYPAGE_RECEIVED_REVIEWS_FOUND,
                mypageReviewService.getReceivedReviews(user.getId(), pageable)
        );
    }
}
