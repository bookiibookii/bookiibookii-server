package com.example.bookiibookii.global.auth.dto.req;

import lombok.Getter;


@Getter
public class AuthRequestDTO {
    private String socialType;
    private String token;
    private String authorizationCode; // Apple 전용: refresh_token 교환 및 탈퇴 시 revoke에 사용

    public static AuthRequestDTO of(String socialType, String token) {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.socialType = socialType;
        dto.token = token;
        return dto;
    }

    public record RefreshRequest(String refreshToken) {}

}

