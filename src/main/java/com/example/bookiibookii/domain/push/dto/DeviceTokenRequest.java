package com.example.bookiibookii.domain.push.dto;

import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class DeviceTokenRequest {

    private DeviceTokenRequest() {
    }

    public record Register(
            @NotBlank
            @Size(max = 512)
            String token,

            @NotNull
            DevicePlatform platform
    ) {
    }

    public record Deactivate(
            @NotBlank
            @Size(max = 512)
            String token
    ) {
    }
}
