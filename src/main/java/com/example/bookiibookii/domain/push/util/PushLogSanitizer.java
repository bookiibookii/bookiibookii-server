package com.example.bookiibookii.domain.push.util;

public final class PushLogSanitizer {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private PushLogSanitizer() {
    }

    public static String safeExceptionMessage(String message, String token) {
        if (message == null || message.isBlank()) {
            return "<no-message>";
        }
        String tokenSanitized = token == null || token.isBlank()
                ? message
                : message.replace(token, "[REDACTED_TOKEN]");
        return safeMessage(tokenSanitized);
    }

    public static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "<no-message>";
        }
        String sanitized = message
                .replaceAll("(?s)-----BEGIN PRIVATE KEY-----.*?-----END PRIVATE KEY-----", "[REDACTED_PRIVATE_KEY]")
                .replace('\n', ' ')
                .replace('\r', ' ');
        return sanitized.length() <= MAX_MESSAGE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    public static String maskedToken(String token) {
        if (token == null || token.length() < 9) {
            return "********";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
