package com.example.bookiibookii.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.share")
public record ShareWebProperties(
        String webBaseUrl
) {
}
