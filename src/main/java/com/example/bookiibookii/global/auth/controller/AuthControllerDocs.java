package com.example.bookiibookii.global.auth.controller;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.auth.dto.req.AuthRequestDTO;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "소셜 로그인",
            description = """
            Android 앱에서 소셜 SDK 로그인 성공 후 호출하는 API입니다.

            - socialType: KAKAO | GOOGLE
            - token:
              - KAKAO → Access Token
              - GOOGLE → ID Token

            인증 성공 시 Access Token, Refresh Token을 반환합니다.
            """
    )
    @PostMapping("/login")
    ApiResponse<AuthResponseDTO.TokenResponse> socialLogin(@RequestBody AuthRequestDTO request);

    @Operation(
            summary = "Access Token 재발급",
            description = """
            만료된 Access Token을 Refresh Token을 이용해 재발급합니다.

            - Authorization 헤더에 Refresh Token을 전달해야 합니다.
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
                    description = "Refresh Token이 없거나 유효하지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Refresh Token 만료"
            )
    })
    @PostMapping("/refresh")
    ApiResponse<AuthResponseDTO.TokenResponse> refresh(@RequestParam String requestRefreshToken, HttpServletRequest request);

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

    @Operation(
            summary = "회원탈퇴",
            description = "회원탈퇴 처리 API입니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "회원탈퇴 성공"
    )
    @DeleteMapping("/withdraw")
    ApiResponse<Void> withdraw(HttpServletRequest request);
}
