package com.example.bookiibookii.domain.recommendation.controller;

import com.example.bookiibookii.domain.recommendation.dto.res.RecommendationResponseDTO;
import com.example.bookiibookii.domain.recommendation.service.RecommendationService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController implements RecommendationControllerDocs{
    private final RecommendationService recommendationService;

    // 사용자 태그 기반 부키메이트 추천 API
    @Override
    @GetMapping("/bookmates")
    public ApiResponse<List<RecommendationResponseDTO.BookmateDto>> recommendBookmates(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<RecommendationResponseDTO.BookmateDto> result =
                recommendationService.findRecommendBookmates(user.getId());

        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // TODO : 그룹 추천 API => UserTag = GroupTag (GENRE > METHOD > VIBE > SPEED)
}
