package com.example.bookiibookii.global.auth.controller;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.auth.dto.req.AuthRequestDTO;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "소셜 로그인",
            description = """
            Android 앱에서 소셜 SDK 로그인 성공 후 호출하는 API입니다.

            - socialType: KAKAO | GOOGLE
            - token:
              - KAKAO → Access Token
                Test: https://kauth.kakao.com/oauth/authorize?client_id=1d55fc7fcf8b8f41295f87d737babee3&redirect_uri=https://bookii.gyeonseo.com/kakao/callback&response_type=code
              - GOOGLE → ID Token

            인증 성공 시 Access Token, Refresh Token을 반환합니다.
            """
    )
    @PostMapping("/login")
    ApiResponse<AuthResponseDTO.LoginResponse> socialLogin(@RequestBody AuthRequestDTO request);

    @Operation(
            summary = "Access Token 재발급",
            description = """
            만료된 Access Token을 Refresh Token을 이용해 재발급합니다.

            - Authorization 헤더에 만료된 Access Token을 전달해야 합니다.
            - requestRefreshToken 파라미터로 Refresh Token을 전달해야 합니다.
            - Refresh Token이 유효하지 않으면 재발급에 실패합니다.
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Access Token 재발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Refresh Token이 없거나 유효하지 않습니다."
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Access Token을 찾을 수 없습니다."
            )
    })
    @PostMapping("/refresh")
    ApiResponse<AuthResponseDTO.TokenResponse> refresh(
            @RequestBody AuthRequestDTO.RefreshRequest requestRefreshToken, HttpServletRequest request);

    @Operation(
            summary = "로그아웃",
            description = """
            로그아웃 처리 API입니다.

            - 서버에서 Refresh Token을 삭제합니다.
            """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공"
    )
    @PostMapping("/logout")
    ApiResponse<Void> logout(HttpServletRequest request);


}
