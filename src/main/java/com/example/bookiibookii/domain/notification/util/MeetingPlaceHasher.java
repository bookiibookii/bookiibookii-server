package com.example.bookiibookii.domain.notification.util;

import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class MeetingPlaceHasher {

    private MeetingPlaceHasher() {
    }

    public static String hash(MeetingRequestDTO request) {
        return hash(
                request.placeName(),
                request.address(),
                request.zipCode(),
                request.x(),
                request.y(),
                request.addressDetail()
        );
    }

    private static String hash(Object... values) {
        StringBuilder source = new StringBuilder();
        for (Object value : values) {
            String text = normalize(value);
            source.append(text.length()).append(':').append(text).append('|');
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String normalize(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        return value.toString();
    }
}
