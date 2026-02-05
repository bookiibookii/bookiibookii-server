package com.example.bookiibookii.domain.notification.controller;

import com.example.bookiibookii.domain.notification.dto.NotificationResDTO;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.exception.code.NotificationSuccessCode;
import com.example.bookiibookii.domain.notification.service.NotificationService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController implements NotificationControllerDocs{

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<NotificationResDTO.NotificationListRes> getNotifications(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestParam NotificationCategory category,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        var res = notificationService.getNotifications(user.getId(), category, cursor, size);
        return ApiResponse.onSuccess(NotificationSuccessCode.NOTIFICATION_LIST_OK, res);
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResDTO.NotificationReadRes> markAsRead(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long notificationId
    ) {
        var res = notificationService.markAsRead(user.getId(), notificationId);
        return ApiResponse.onSuccess(NotificationSuccessCode.NOTIFICATION_READ_OK, res);
    }
}
