package com.example.bookiibookii.domain.book.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class BookAuthorMapper {
    private static final Pattern PREFIX = Pattern.compile("^(?:지은이|글쓴이|저자|글|저)\\s*[:：]\\s*");
    private static final Pattern SUFFIX = Pattern.compile("(?:\\s*\\((?:지은이|저자|글)\\)|\\s+(?:지음|지은이|글|저))$");

    private static final Pattern LATIN_NAME = Pattern.compile("\\p{IsLatin}+(?:[-'’]\\p{IsLatin}+)*");
    private static final Pattern INITIALS = Pattern.compile("(?:\\p{IsLatin}\\.\\s*)+");

    public String mapFirstWriterOnly(String raw) {
        if (raw == null) return null;
        String credit = PREFIX.matcher(raw.strip()).replaceFirst("").split("[;；]", 2)[0];
        String[] parts = credit.split("[,，]", 3);
        String first = parts[0].strip();
        // 'Austen, Jane'이나 'Tolkien, J. R. R.'처럼 성을 먼저 적은 서지 표기를 보존한다.
        if (parts.length > 1 && LATIN_NAME.matcher(first).matches()) {
            String given = SUFFIX.matcher(parts[1].strip()).replaceFirst("").strip();
            if (LATIN_NAME.matcher(given).matches() || INITIALS.matcher(given).matches()) {
                return first + ", " + given;
            }
        }
        first = SUFFIX.matcher(first).replaceFirst("").strip();
        return first.isEmpty() ? null : first;
    }
}
