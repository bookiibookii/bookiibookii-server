package com.example.bookiibookii.global.auth.dto;

import lombok.Builder;

public class AuthResDTO {
    @Builder
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            Long userId
    ){}
}
