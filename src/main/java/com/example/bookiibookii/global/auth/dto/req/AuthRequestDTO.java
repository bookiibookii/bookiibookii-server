package com.example.bookiibookii.global.auth.dto.req;

import lombok.Getter;


@Getter
public class AuthRequestDTO {
    private String socialType;
    private String token;

    public record RefreshRequest(String refreshToken) {}

}

