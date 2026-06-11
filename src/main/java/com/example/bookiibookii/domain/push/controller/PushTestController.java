package com.example.bookiibookii.domain.push.controller;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.dto.PushTestRequest;
import com.example.bookiibookii.domain.push.service.PushService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev"})
@RequestMapping("/api/dev/push")
@RequiredArgsConstructor
public class PushTestController {

    private final PushService pushService;

    @PostMapping
    public ApiResponse<Void> send(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody PushTestRequest request
    ) {
        Long targetUserId = request.userId() == null ? user.getId() : request.userId();
        pushService.sendToUser(targetUserId, new PushMessage(request.title(), request.body(), request.data()));
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
