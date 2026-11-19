package com.example.bookiibookii.domain.book.catalog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.example.bookiibookii.domain.book.catalog.LibraryTransport.unavailable;

@Component
public class LibraryCatalogClient {
    private final LibraryTransport naru;
    private final LibraryTransport nl;
    private static final Pattern PAGE_COUNT = Pattern.compile("(?i)([1-9][0-9]{0,8})\\s*(?:p\\.?|pages?|쪽|면)?");

    @Autowired
    LibraryCatalogClient(ObjectMapper json, @Value("${DATA4LIBRARY_AUTH_KEY:}") String naruKey,
                         @Value("${NLGO_CERT_KEY:}") String nlKey) {
        this(new LibraryTransport(json, "https://data4library.kr/api/", "authKey", naruKey, 1000, Duration.ofSeconds(4)),
                new LibraryTransport(json, "https://www.nl.go.kr/seoji/", "cert_key", nlKey, 1000, Duration.ofSeconds(4)));
    }

    LibraryCatalogClient(LibraryTransport naru, LibraryTransport nl) {
        this.naru = naru;
        this.nl = nl;
    }

    List<CatalogBook> naruDetail(String isbn) {
        var root = naru.get("srchDtlList", Map.of("isbn13", isbn, "format", "json", "loaninfoYN", "N"));
        return naruRows(naruResponse(root), "detail", "book").stream().filter(b -> isbn.equals(b.isbn())).toList();
    }

    public record SearchPage(int total, List<CatalogBook> books) {}

    public SearchPage keyword(String keyword, int page, int size) {
        return search("keyword", keyword, page, size);
    }

    List<CatalogBook> naruByIsbn(String isbn) {
        SearchPage page = search("isbn13", isbn, 1, 10);
        // An unseen duplicate can carry a conflicting audience code.
        if (page.total != page.books.size()) throw unavailable();
        return page.books.stream().filter(b -> isbn.equals(b.isbn())).toList();
    }

    private SearchPage search(String field, String value, int page, int size) {
        var response = naruResponse(naru.get("srchBooks", Map.of(field, value, "pageNo", String.valueOf(page),
                "pageSize", String.valueOf(size), "format", "json")));
        int total = count(response, "numFound");
        var books = naruRows(response, "docs", "doc");
        long offset = (long) (page - 1) * size;
        long remaining = Math.max(0L, total - offset);
        if (books.size() > Math.min(remaining, size) || (remaining > 0 && books.isEmpty())) throw unavailable();
        return new SearchPage(total, books);
    }

    private static JsonNode naruResponse(JsonNode root) {
        var response = root.path("response");
        if (!response.isObject() || response.hasNonNull("error") || root.hasNonNull("error")) throw unavailable();
        return response;
    }

    private static List<CatalogBook> naruRows(JsonNode response, String field, String wrapper) {
        var rows = response.path(field);
        if (!rows.isArray()) throw unavailable();
        List<CatalogBook> books = new ArrayList<>();
        for (var row : rows) {
            var b = row.path(wrapper);
            if (!b.isObject()) throw unavailable();
            books.add(new CatalogBook(isbn(b, "isbn13"), text(b, "bookname"), text(b, "authors"),
                    text(b, "publisher"), CatalogBook.externalUrl(text(b, "bookImageURL")), text(b, "description"),
                    null, wrapper.equals("doc") ? CatalogBook.externalUrl(text(b, "bookDtlUrl")) : "", text(b, "class_no"),
                    text(b, "class_nm"), text(b, "addition_symbol")));
        }
        return books;
    }

    List<CatalogBook> nationalDetail(String isbn) {
        var root = nl.get("SearchApi.do", Map.of("isbn", isbn, "result_style", "json", "page_no", "1", "page_size", "10"));
        var docs = root.path("docs");
        if (root.hasNonNull("error_code") || root.hasNonNull("ERROR_CODE") || !docs.isArray()) throw unavailable();
        int total = count(root, "TOTAL_COUNT");
        if (total != docs.size()) throw unavailable();
        var matches = new ArrayList<CatalogBook>();
        for (var b : docs) {
            if (!b.isObject()) throw unavailable();
            if (!isbn.equals(isbn(b, "EA_ISBN"))) continue;
            matches.add(new CatalogBook(isbn, text(b, "TITLE"), text(b, "AUTHOR"), text(b, "PUBLISHER"),
                    CatalogBook.externalUrl(text(b, "TITLE_URL")), text(b, "BOOK_INTRODUCTION"), pages(text(b, "PAGE")),
                    "", text(b, "KDC"), "", text(b, "EA_ADD_CODE")));
        }
        return matches;
    }

    private static int count(JsonNode node, String field) {
        try {
            int count = Integer.parseInt(text(node, field));
            if (count >= 0) return count;
        } catch (NumberFormatException ignored) {
        }
        throw unavailable();
    }

    static Integer pages(String value) {
        var match = PAGE_COUNT.matcher(value);
        return match.matches() ? Integer.valueOf(match.group(1)) : null;
    }

    private static String isbn(JsonNode b, String field) {
        return text(b, field).replace("-", "");
    }

    private static String text(JsonNode b, String field) {
        var v = b.path(field);
        return v.isString() || v.isNumber() ? v.asString().strip() : "";
    }
}
