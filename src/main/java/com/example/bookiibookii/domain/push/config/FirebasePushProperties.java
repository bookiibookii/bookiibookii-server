package com.example.bookiibookii.domain.push.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "push.firebase")
public record FirebasePushProperties(
        boolean enabled,
        String credentialsPath,
        String projectId
) {
}