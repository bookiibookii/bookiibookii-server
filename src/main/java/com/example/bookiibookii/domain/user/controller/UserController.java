package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.service.UserService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs{
    private final UserService userService;

    // 닉네임 검증
    @Override
    @PostMapping("/api/users/name-validation")
    public ApiResponse<Map<String, Boolean>> validateNickname(
            @NotNull @RequestParam String nickname
    ) {
        boolean isAvailable = userService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, Map.of("isAvailable", isAvailable));
    }

}
