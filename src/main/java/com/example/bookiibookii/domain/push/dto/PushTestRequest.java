package com.example.bookiibookii.domain.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PushTestRequest(
        Long userId,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotBlank
        @Size(max = 1000)
        String body,

        Map<String, String> data
) {
}
