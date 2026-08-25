package com.example.bookiibookii.domain.review.controller;

import com.example.bookiibookii.domain.review.dto.res.MypageReviewResponseDTO;
import com.example.bookiibookii.domain.review.exception.code.ReviewSuccessCode;
import com.example.bookiibookii.domain.review.service.MypageReviewService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MypageReviewControllerTest {

    @Mock
    private MypageReviewService mypageReviewService;

    private MypageReviewController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new MypageReviewController(mypageReviewService);
        user = User.builder()
                .id(1L)
                .socialType(SocialType.KAKAO)
                .socialId("mypage-review-controller-test")
                .build();
    }

    @Test
    void writtenReviewsUseWrittenReviewsSuccessCode() {
        PageRequest pageable = PageRequest.of(0, 20);
        MypageReviewResponseDTO.WrittenReviews result =
                new MypageReviewResponseDTO.WrittenReviews(
                        0,
                        List.of(),
                        new MypageReviewResponseDTO.PageInfo(0, 20, 0, false)
                );
        when(mypageReviewService.getWrittenReviews(user.getId(), user.getLastResetAt(), pageable)).thenReturn(result);

        ApiResponse<MypageReviewResponseDTO.WrittenReviews> response =
                controller.getWrittenReviews(user, pageable);

        assertThat(response.getCode()).isEqualTo(ReviewSuccessCode.MYPAGE_WRITTEN_REVIEWS_FOUND.getCode());
        assertThat(response.getMessage()).isEqualTo(ReviewSuccessCode.MYPAGE_WRITTEN_REVIEWS_FOUND.getMessage());
        assertThat(response.getResult()).isSameAs(result);
    }

    @Test
    void receivedReviewsUseReceivedReviewsSuccessCode() {
        PageRequest pageable = PageRequest.of(0, 20);
        MypageReviewResponseDTO.ReceivedReviews result =
                new MypageReviewResponseDTO.ReceivedReviews(
                        0,
                        List.of(),
                        new MypageReviewResponseDTO.PageInfo(0, 20, 0, false)
                );
        when(mypageReviewService.getReceivedReviews(user.getId(), user.getLastResetAt(), pageable)).thenReturn(result);

        ApiResponse<MypageReviewResponseDTO.ReceivedReviews> response =
                controller.getReceivedReviews(user, pageable);

        assertThat(response.getCode()).isEqualTo(ReviewSuccessCode.MYPAGE_RECEIVED_REVIEWS_FOUND.getCode());
        assertThat(response.getMessage()).isEqualTo(ReviewSuccessCode.MYPAGE_RECEIVED_REVIEWS_FOUND.getMessage());
        assertThat(response.getResult()).isSameAs(result);
    }
}
