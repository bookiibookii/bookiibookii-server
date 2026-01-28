package com.example.bookiibookii.domain.notification.util;

import java.text.Normalizer;

public final class KeywordNormalizer {

    private KeywordNormalizer() {}

    // 앞뒤 공백 trim (DB 저장 용도)
    public static String display(String raw) {
        if (raw == null) return null;
        return raw.trim();
    }

    // 앞뒤 trim + 공백제거 + 소문자 + 특수문자 제거 (keyword-저자,도서명 매칭 용도)
    public static String normalize(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFKC);
        s = s.replaceAll("\\s+", "");
        s = s.toLowerCase();
        s = s.replaceAll("[^0-9a-z가-힣]", "");

        return s;
    }

    public static String prefix2(String normalized) {
        if (normalized == null || normalized.isBlank()) return null;
        return normalized.length() >= 2 ? normalized.substring(0, 2) : normalized;
    }
}
