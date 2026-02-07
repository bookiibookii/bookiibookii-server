package com.example.bookiibookii.global.auth.dto.res;

import lombok.Builder;

public class AuthResponseDTO {
    @Builder
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            Long userId
    ){}

    @Builder
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            Long userId,
            Boolean onboardingDone
    ){}
}
