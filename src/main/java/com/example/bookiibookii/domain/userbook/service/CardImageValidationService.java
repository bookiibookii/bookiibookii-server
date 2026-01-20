package com.example.bookiibookii.domain.userbook.service;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CardImageValidationService {

    private static final Pattern S3_KEY_PATTERN = Pattern.compile("^image/cards/(\\d+)/([a-f0-9-]{36})$");

    /**
     * s3Key 형식 검증 및 cardId 일치 확인
     * @param s3Key 검증할 S3 키
     * @param cardId 카드 ID
     * @return 검증 성공 여부
     */
    public boolean isValidS3Key(String s3Key, Long cardId) {
        if (s3Key == null || s3Key.isBlank()) {
            return false;
        }

        // 형식 검증: image/cards/{cardId}/{uuid}
        java.util.regex.Matcher matcher = S3_KEY_PATTERN.matcher(s3Key);
        if (!matcher.matches()) {
            return false;
        }

        // cardId 일치 확인
        try {
            Long extractedCardId = Long.parseLong(matcher.group(1));
            if (!extractedCardId.equals(cardId)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // UUID 형식 검증
        String uuidString = matcher.group(2);
        try {
            UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    /**
     * s3Key에서 cardId 추출
     * @param s3Key S3 키
     * @return cardId (형식이 올바르지 않으면 null)
     */
    public Long extractCardIdFromS3Key(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }

        java.util.regex.Matcher matcher = S3_KEY_PATTERN.matcher(s3Key);
        if (!matcher.matches()) {
            return null;
        }

        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
