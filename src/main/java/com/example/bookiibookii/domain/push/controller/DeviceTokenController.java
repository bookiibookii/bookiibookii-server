package com.example.bookiibookii.domain.push.controller;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.service.DeviceTokenService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public ApiResponse<Void> register(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody DeviceTokenRequest.Register request
    ) {
        deviceTokenService.register(user.getId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @DeleteMapping
    public ApiResponse<Void> deactivate(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody DeviceTokenRequest.Deactivate request
    ) {
        deviceTokenService.deactivate(user.getId(), request.token());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
