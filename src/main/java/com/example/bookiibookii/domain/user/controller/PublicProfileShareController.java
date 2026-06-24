package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.res.PublicProfileResponseDTO;
import com.example.bookiibookii.domain.user.exception.code.UserSuccessCode;
import com.example.bookiibookii.domain.user.service.ProfileShareService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/profiles")
@RequiredArgsConstructor
public class PublicProfileShareController implements PublicProfileShareControllerDocs {

    private final ProfileShareService profileShareService;

    @GetMapping("/{shareToken}")
    public ApiResponse<PublicProfileResponseDTO> getPublicProfile(
            @PathVariable String shareToken
    ) {
        PublicProfileResponseDTO response = profileShareService.getPublicProfile(shareToken);
        return ApiResponse.onSuccess(UserSuccessCode.PUBLIC_PROFILE_FOUND, response);
    }
}
