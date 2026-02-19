package com.example.bookiibookii.domain.book.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BookAuthorMapper {

    private static final Pattern AUTHOR_BEFORE_ROLE =
            Pattern.compile("^\\s*([^,(]+?)\\s*\\(\\s*지은이\\s*\\)?\\s*$");

    public String mapFirstWriterOnly(String raw) {
        if (raw == null) return null;

        String s = raw.trim();
        if (s.isEmpty()) return null;

        String first = s.split("\\s*,\\s*", 2)[0].trim();
        if (first.isEmpty()) return null;

        int idx = first.indexOf("(지은이");
        if (idx >= 0) {
            first = first.substring(0, idx).trim();
            return first.isEmpty() ? null : first;
        }

        Matcher m = AUTHOR_BEFORE_ROLE.matcher(first);
        if (m.matches()) {
            String name = m.group(1).trim();
            return name.isEmpty() ? null : name;
        }

        return first;
    }
}
