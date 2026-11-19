package com.example.bookiibookii.domain.book.catalog;

import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** One limiter per provider per JVM, shared by detail and search calls. */
final class LibraryTransport {
    private final HttpClient http;
    private final ObjectMapper json;
    private final String base;
    private final String keyParam;
    private final String key;
    private final long intervalMillis;
    private final Duration timeout;
    private final Semaphore slot = new Semaphore(1, true);
    private long lastStart;

    LibraryTransport(ObjectMapper json, String base, String keyParam, String key, long intervalMillis, Duration timeout) {
        this.http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build();
        this.json = json;
        this.base = base;
        this.keyParam = keyParam;
        this.key = key;
        this.intervalMillis = intervalMillis;
        this.timeout = timeout;
    }

    JsonNode get(String path, Map<String, String> params) {
        if (key.isBlank()) throw unavailable();
        String query = params.entrySet().stream().map(e -> e.getKey() + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path + "?" + query + "&" + keyParam + "=" + encode(key)))
                .timeout(timeout).GET().build();
        try {
            if (!slot.tryAcquire(timeout.toMillis(), TimeUnit.MILLISECONDS)) throw unavailable();
            try {
                for (int attempt = 0; attempt < 2; attempt++) {
                    if (lastStart != 0) {
                        long remaining = TimeUnit.MILLISECONDS.toNanos(intervalMillis) - (System.nanoTime() - lastStart);
                        if (remaining > 0) TimeUnit.NANOSECONDS.sleep(remaining);
                    }
                    lastStart = System.nanoTime();
                    try {
                        var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
                        if (response.statusCode() >= 500 && attempt == 0) continue;
                        if (response.statusCode() != 200) throw unavailable();
                        return json.readTree(response.body());
                    } catch (IOException ex) {
                        if (attempt == 1) throw unavailable();
                    }
                }
            } finally {
                slot.release();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (JacksonException ex) {
            // Deliberately discard upstream text and exceptions containing authenticated URLs.
        }
        throw unavailable();
    }

    static BookException unavailable() {
        return new BookException(BookErrorCode.CATALOG_UNAVAILABLE);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
