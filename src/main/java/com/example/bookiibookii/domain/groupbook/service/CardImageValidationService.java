package com.example.bookiibookii.domain.groupbook.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CardImageValidationService {

    private static final Pattern S3_KEY_PATTERN = Pattern.compile("^image/cards/([a-f0-9-]{36})$");

    /**
     * s3Key 형식 검증 (UUID만 사용)
     * @param s3Key 검증할 S3 키
     * @return 검증 성공 여부
     */
    public boolean isValidS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return false;
        }

        // 형식 검증: image/cards/{uuid}
        java.util.regex.Matcher matcher = S3_KEY_PATTERN.matcher(s3Key);
        if (!matcher.matches()) {
            return false;
        }

        // UUID 형식 검증
        String uuidString = matcher.group(1);
        try {
            UUID.fromString(uuidString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
