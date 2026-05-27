package com.example.bookiibookii.global.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord.webhook")
public record DiscordWebhookProperties(
        boolean enabled,
        String url
) {}
