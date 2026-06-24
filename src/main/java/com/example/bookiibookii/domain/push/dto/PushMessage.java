package com.example.bookiibookii.domain.push.dto;

import java.util.Map;

public record PushMessage(
        String title,
        String body,
        Map<String, String> data
) {
    public PushMessage {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}