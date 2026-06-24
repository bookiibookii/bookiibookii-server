package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.res.MypageReviewResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "BookReview", description = "책 리뷰 및 독서카드 관련 API")
@RequestMapping("/api/mypage/reviews")
public interface MypageReviewControllerDocs {

    @GetMapping("/written")
    @Operation(
            summary = "마이페이지 작성한 후기 조회",
            description = """
            로그인 사용자가 작성한 책 후기만 최신순으로 조회합니다.
            파트너에게 작성한 파트너 후기는 포함하지 않습니다.
            페이지 번호는 0부터 시작하며 기본 size는 20입니다.
            """
    )
    ApiResponse<MypageReviewResponseDTO.WrittenReviews> getWrittenReviews(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @ParameterObject Pageable pageable
    );

    @GetMapping("/received")
    @Operation(
            summary = "마이페이지 받은 후기 조회",
            description = """
            로그인 사용자가 받은 파트너 후기만 최신순으로 조회합니다.
            책 후기는 포함하지 않으며 comment와 partnerReviewType은 null일 수 있습니다.
            positiveCount는 BOOM_UP 후기 수입니다.
            페이지 번호는 0부터 시작하며 기본 size는 20입니다.
            """
    )
    ApiResponse<MypageReviewResponseDTO.ReceivedReviews> getReceivedReviews(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user,
            @ParameterObject Pageable pageable
    );
}
