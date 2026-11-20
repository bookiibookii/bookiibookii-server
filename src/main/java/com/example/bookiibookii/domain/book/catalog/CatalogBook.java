package com.example.bookiibookii.domain.book.catalog;

import java.net.URI;
import java.util.regex.Pattern;

/** Transient metadata for one edition. Descriptions are never persisted. */
public record CatalogBook(String isbn, String title, String author, String publisher, String cover,
                          String description, Integer pages, String link, String kdc, String ddc, String classification,
                          String additionSymbol) {

    private static final Pattern ISBN = Pattern.compile("97[89][0-9]{10}");

    private static final Pattern AUTH_QUERY = Pattern.compile("(?:^|[&;])(?:authKey|cert_key|ttbkey)=", Pattern.CASE_INSENSITIVE);

    public static boolean validIsbn(String isbn) {
        if (isbn == null || !ISBN.matcher(isbn).matches()) return false;
        int sum = 0;
        for (int i = 0; i < 13; i++) sum += (isbn.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return sum % 10 == 0;
    }

    CatalogBook supplement(CatalogBook other) {
        if (!isbn.equals(other.isbn)) throw new IllegalArgumentException("ISBN mismatch");
        return new CatalogBook(isbn, first(title, other.title), first(author, other.author),
                first(publisher, other.publisher), first(cover, other.cover), first(description, other.description),
                pages == null ? other.pages : pages, first(link, other.link), first(kdc, other.kdc),
                first(ddc, other.ddc), first(classification, other.classification), first(additionSymbol, other.additionSymbol));
    }

    static String first(String value, String fallback) {
        return value.isBlank() ? fallback : value;
    }

    static String externalUrl(String value) {
        return externalUrl(value, false);
    }

    static String coverUrl(String value) {
        return externalUrl(value, true);
    }

    private static String externalUrl(String value, boolean cover) {
        try {
            URI uri = URI.create(value);
            if (uri.getQuery() != null && AUTH_QUERY.matcher(uri.getQuery()).find()) return "";
            boolean http = "http".equalsIgnoreCase(uri.getScheme());
            if ((!http && !"https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) return "";
            if (cover && http && "image.aladin.co.kr".equalsIgnoreCase(uri.getHost())
                    && (uri.getPort() == -1 || uri.getPort() == 80)) {
                int authorityEnd = "http://".length() + uri.getRawAuthority().length();
                return "https://" + uri.getHost() + value.substring(authorityEnd);
            }
            return value;
        } catch (IllegalArgumentException ignored) {
            // Malformed provider metadata is a missing field.
        }
        return "";
    }
}
