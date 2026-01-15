package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.domain.user.enums.SocialType;

public interface SocialTokenVerifier {

    // 처리 가능한 socialType인지 확인
    boolean supports(SocialType socialType);

    // 소셜 토큰 검증 후 사용자 정보 반환
    SocialUserInfo verify(String token);
}
