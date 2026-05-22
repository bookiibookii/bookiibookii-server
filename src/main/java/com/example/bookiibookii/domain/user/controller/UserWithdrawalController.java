package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.code.UserSuccessCode;
import com.example.bookiibookii.domain.user.service.UserWithdrawalService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class UserWithdrawalController implements UserWithdrawalControllerDocs {

    private final UserWithdrawalService userWithdrawalService;

    @Override
    @PostMapping("/api/users/me/withdrawal")
    public ApiResponse<Void> withdraw(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.WithdrawalReqDTO request
    ) {
        userWithdrawalService.withdraw(user, request.reason(), request.customReason());
        return ApiResponse.onSuccess(UserSuccessCode.WITHDRAWAL_SUCCESS, null);
    }
}
