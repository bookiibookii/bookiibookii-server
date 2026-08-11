package com.example.bookiibookii.domain.push.dto;

import com.example.bookiibookii.domain.push.enums.DevicePlatform;

public record ActiveDeviceToken(
        Long id,
        String token,
        DevicePlatform platform
) {
}
