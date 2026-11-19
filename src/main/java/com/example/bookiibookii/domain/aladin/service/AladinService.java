package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.dto.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.catalog.CatalogBook;
import com.example.bookiibookii.domain.book.catalog.LibraryCatalogClient;
import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.domain.book.service.LibraryCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AladinService {
    private final LibraryCatalogClient catalog;
    private final LibraryCategoryMapper categories;
    private final BookAuthorMapper authors;

    public AladinSearchBooksResDTO searchBooks(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank() || page < 1 || size < 1) {
            throw new BookException(BookErrorCode.INVALID_SEARCH);
        }
        var result = catalog.keyword(keyword, page, size);
        var blocked = result.books().stream().filter(categories::blockedInCatalog)
                .map(CatalogBook::isbn).collect(Collectors.toSet());
        var seen = new HashSet<String>();
        var books = new ArrayList<BookResDTO>();
        for (var book : result.books()) {
            if (!CatalogBook.validIsbn(book.isbn()) || blocked.contains(book.isbn()) || !seen.add(book.isbn())) continue;
            var category = categories.catalogCategory(book.kdc());
            books.add(BookResDTO.builder().title(book.title()).author(Objects.toString(authors.mapFirstWriterOnly(book.author()), ""))
                    .publisher(book.publisher()).image(book.cover()).isbn13(book.isbn()).link(book.link())
                    .category(category).categoryLabel(category.getLabel()).build());
        }
        // The existing contract counts raw upstream hits, before filtering each page.
        return AladinSearchBooksResDTO.builder().books(books).totalResults(result.total())
                .totalPage((int) Math.ceil(result.total() / (double) size)).build();
    }
}
