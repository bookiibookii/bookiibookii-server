package com.example.bookiibookii.domain.book.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class BookAuthorMapper {
    private static final Pattern PREFIX = Pattern.compile("^(?:지은이|글쓴이|저자|글|저)\\s*[:：]\\s*");
    private static final Pattern SUFFIX = Pattern.compile("(?:\\s*\\((?:지은이|저자|글)\\)|\\s+(?:지음|지은이|글|저))$");

    public String mapFirstWriterOnly(String raw) {
        if (raw == null) return null;
        String first = raw.split("[;,；]", 2)[0].strip();
        first = SUFFIX.matcher(PREFIX.matcher(first).replaceFirst("")).replaceFirst("").strip();
        return first.isEmpty() ? null : first;
    }
}
