package com.example.bookiibookii.global.auth.dto.res;

import com.example.bookiibookii.domain.user.enums.Role;
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
            String onboardingStatus,
            Role role
    ){}
}
