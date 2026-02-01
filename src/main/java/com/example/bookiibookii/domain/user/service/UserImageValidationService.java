package com.example.bookiibookii.domain.user.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class UserImageValidationService {

    /** image/users/{userId}/{uuid} */
    private static final Pattern S3_KEY_PATTERN = Pattern.compile("^image/users/(\\d+)/([a-f0-9-]{36})$");

    /**
     * s3Key 형식 검증 (image/users/{userId}/{uuid})
     */
    public boolean isValidS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return false;
        }
        java.util.regex.Matcher matcher = S3_KEY_PATTERN.matcher(s3Key);
        if (!matcher.matches()) {
            return false;
        }
        try {
            UUID.fromString(matcher.group(2));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
