package com.example.bookiibookii.domain.book.catalog;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.domain.book.service.LibraryCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class HybridBookCatalog {
    private final LibraryCatalogClient client;
    private final LibraryCategoryMapper categories;
    private final BookAuthorMapper authors;

    public Book lookup(String isbn) {
        if (!CatalogBook.validIsbn(isbn)) throw new BookException(BookErrorCode.INVALID_ISBN);
        Lookup detail = naruDetail(isbn);
        // 국중도만 쪽수를 제공한다. 호출 실패는 미검색이나 쪽수 누락으로 취급하지 않는다.
        List<CatalogBook> national = client.nationalDetail(isbn);
        if (national.isEmpty()) {
            if (detail.failed()) throw LibraryTransport.unavailable();
            throw new BookException(!detail.books().isEmpty() ? BookErrorCode.PAGES_UNAVAILABLE : BookErrorCode.BOOK_NOT_FOUND);
        }
        var pageCounts = national.stream().map(CatalogBook::pages).filter(Objects::nonNull).distinct().toList();
        if (pageCounts.size() != 1 || pageCounts.get(0) <= 0) {
            throw new BookException(BookErrorCode.PAGES_UNAVAILABLE);
        }

        // 상세 URL은 srchBooks의 같은 ISBN 레코드에서만 가져온다.
        List<CatalogBook> search = client.naruByIsbn(isbn);
        var sources = Stream.of(detail.books(), search, national).flatMap(List::stream).toList();
        if (sources.stream().anyMatch(categories::blockedInCatalog)) throw new BookException(BookErrorCode.BLOCKED_CATEGORY);
        CatalogBook book = sources.stream().reduce(CatalogBook::supplement).orElseThrow();
        String author = sources.stream().map(CatalogBook::author).map(authors::mapFirstWriterOnly)
                .filter(Objects::nonNull).findFirst().orElse(null);
        if (book.title().isEmpty() || author == null || book.publisher().isEmpty() || book.cover().isEmpty()) {
            if (detail.failed()) throw LibraryTransport.unavailable();
            throw new BookException(BookErrorCode.INCOMPLETE_BOOK);
        }
        // Book의 문자열 컬럼 길이를 초과한 URL이나 이름을 잘라서 저장하지 않는다.
        if (Stream.of(book.title(), author, book.publisher(), book.cover())
                .anyMatch(value -> value.codePointCount(0, value.length()) > 255)) {
            throw new BookException(BookErrorCode.INCOMPLETE_BOOK);
        }
        String link = book.link().codePointCount(0, book.link().length()) <= 255 ? book.link() : "";
        CustomCategory category = categories.catalogCategory(book.kdc());
        return Book.builder().isbn13(isbn).title(book.title()).author(author).publisher(book.publisher())
                .totalPages(book.pages()).link(link).image(book.cover()).category(category).build();
    }

    private record Lookup(List<CatalogBook> books, boolean failed) {}

    private Lookup naruDetail(String isbn) {
        try {
            return new Lookup(client.naruDetail(isbn), false);
        } catch (BookException ex) {
            if (ex.getCode() != BookErrorCode.CATALOG_UNAVAILABLE) throw ex;
            return new Lookup(List.of(), true);
        }
    }
}
