package com.example.bookiibookii.domain.recommendation.controller;

import com.example.bookiibookii.domain.recommendation.dto.res.RecommendationResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController implements RecommendationControllerDocs {

    @GetMapping("/groups")
    public ApiResponse<List<RecommendationResponseDTO.RecommendedGroupDto>> recommendGroups(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam(value = "refresh", defaultValue = "false") boolean isRefresh
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
