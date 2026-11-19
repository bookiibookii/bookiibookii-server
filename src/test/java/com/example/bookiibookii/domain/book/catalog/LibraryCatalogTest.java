package com.example.bookiibookii.domain.book.catalog;

import com.example.bookiibookii.domain.aladin.controller.AladinController;
import com.example.bookiibookii.domain.aladin.service.AladinService;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.domain.book.service.LibraryCategoryMapper;
import com.example.bookiibookii.domain.book.service.BookService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LibraryCatalogTest {
    private static final String ISBN = "9788936434267";
    private static final String NATIONAL = """
            {"TOTAL_COUNT":"2","docs":[{"EA_ISBN":"97889364342671","PAGE":"111"},
            {"EA_ISBN":"9788936434267","PAGE":"320 p.","PUBLISHER":"출판사","TITLE":"다른 표기","EA_ADD_CODE":"03810"}]}
            """;
    private static final String BOOK = """
            {"isbn13":"9788936434267","bookname":"제목","authors":"지은이: 김작가 ; 옮긴이: 이역자",
            "bookImageURL":"https://example.org/img","description":"소개","class_no":"813.7"}
            """;
    private static final String DETAIL = "{\"response\":{\"detail\":[{\"book\":" + BOOK + "}]}}";
    private static final String LINK = """
            {"response":{"numFound":1,"docs":[{"doc":{"isbn13":"9788936434267","bookDtlUrl":"https://example.org/edition"}}]}}
            """;
    private static final String NO_DETAIL = "{\"response\":{\"detail\":[]}}";
    private static final String NO_NL = "{\"TOTAL_COUNT\":0,\"docs\":[]}";
    private final String credential = UUID.randomUUID().toString();
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final LibraryCategoryMapper categories = new LibraryCategoryMapper();
    private final BookAuthorMapper authors = new BookAuthorMapper();
    private HttpServer server;
    private LibraryCatalogClient client;
    private HybridBookCatalog catalog;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        var json = new ObjectMapper();
        client = new LibraryCatalogClient(new LibraryTransport(json, base, "authKey", credential, 1, Duration.ofSeconds(2)),
                new LibraryTransport(json, base, "cert_key", credential, 1, Duration.ofSeconds(2)));
        catalog = new HybridBookCatalog(client, categories, authors);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private record Reply(int status, String body) {}

    private void endpoint(String path, Reply... replies) {
        var queue = new ArrayDeque<>(List.of(replies));
        server.createContext("/" + path, exchange -> {
            requests.add(URLDecoder.decode(exchange.getRequestURI().toString(), StandardCharsets.UTF_8)
                    .replace(credential, "[hidden]"));
            var reply = queue.size() > 1 ? queue.removeFirst() : queue.element();
            byte[] body = reply.body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status, body.length);
            try (var stream = exchange.getResponseBody()) { stream.write(body); }
        });
    }

    private void ok(String path, String body) { endpoint(path, new Reply(200, body)); }

    @Test
    void mergesOnlyExactEditionsAndUsesAnActualReturnedDetailLink() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK);
        var b = catalog.lookup(ISBN);
        assertThat(b.getTitle()).isEqualTo("제목");
        assertThat(b.getAuthor()).isEqualTo("김작가");
        assertThat(b.getPublisher()).isEqualTo("출판사");
        assertThat(b.getImage()).isEqualTo("https://example.org/img");
        assertThat(b.getTotalPages()).isEqualTo(320);
        assertThat(b.getLink()).isEqualTo("https://example.org/edition");
        assertThat(b.getCategory()).isEqualTo(CustomCategory.KOREAN_NOVEL);
        assertThat(client.naruDetail(ISBN).get(0).description()).isEqualTo("소개");
        assertThat(requests).anySatisfy(q -> assertThat(q).startsWith("/srchBooks?").contains("isbn13=" + ISBN));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "", "1권", "20-40", "200, 310 p.", "-100", "300 예정"})
    void ambiguousOrMissingPagesRejectRegistrationBeforeInsert(String value) {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL.replace("320 p.", value));
        var repository = mock(BookRepository.class);
        var service = new BookService(repository, catalog, Clock.systemUTC());
        expect(BookErrorCode.PAGES_UNAVAILABLE, () -> service.getOrCreateByIsbn13(ISBN));
        verify(repository).findByIsbn13(ISBN);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void missingExactLinkCannotBeFilledFromAnotherEdition() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK.replace(ISBN, "9788936434120"));
        expect(BookErrorCode.DETAIL_LINK_UNAVAILABLE, () -> catalog.lookup(ISBN));
    }

    @Test
    void conflictingExactPageCountsAreRejected() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL.replace("97889364342671", ISBN));
        expect(BookErrorCode.PAGES_UNAVAILABLE, () -> catalog.lookup(ISBN));
    }

    @Test
    void unusableAuthorFallsBackToSameEditionAndEmptyLinkFallsBackToDuplicate() {
        ok("srchDtlList", DETAIL.replace("지은이: 김작가 ; 옮긴이: 이역자", "저자:"));
        ok("SearchApi.do", NATIONAL.replace("\"EA_ADD_CODE\"", "\"AUTHOR\":\"이작가 지음\",\"EA_ADD_CODE\""));
        ok("srchBooks", LINK.replace("\"numFound\":1", "\"numFound\":2")
                .replace("\"docs\":[", "\"docs\":[{\"doc\":{\"isbn13\":\"" + ISBN + "\"}},"));
        var book = catalog.lookup(ISBN);
        assertThat(book.getAuthor()).isEqualTo("이작가");
        assertThat(book.getLink()).isEqualTo("https://example.org/edition");
    }

    @Test
    void rejectsEditionWithoutCover() {
        ok("srchDtlList", DETAIL.replace("https://example.org/img", ""));
        ok("srchBooks", LINK);
        ok("SearchApi.do", NATIONAL);
        expect(BookErrorCode.INCOMPLETE_BOOK, () -> catalog.lookup(ISBN));
    }

    @Test
    void twoSuccessfulEmptyResponsesAreNotFound() {
        ok("srchDtlList", NO_DETAIL);
        ok("SearchApi.do", NO_NL);
        expect(BookErrorCode.BOOK_NOT_FOUND, () -> catalog.lookup(ISBN));
    }

    @Test
    void http200ErrorAndEmptyPeerAreUnavailableWithoutLeakingUpstreamText() {
        ok("srchDtlList", "{\"response\":{\"error\":\"" + credential + "\"}}");
        ok("SearchApi.do", NO_NL);
        ok("srchBooks", "{\"response\":{\"numFound\":0,\"docs\":[]}}");
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> catalog.lookup(ISBN));
    }

    @Test
    void nationalLibraryErrorCannotBeMisreportedAsMissingPages() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", "{\"error_code\":\"010\",\"docs\":[]}");
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> catalog.lookup(ISBN));
    }

    @Test
    void retriesServerErrorOnceAndUsesSearchMetadataWhenDetailFails() {
        endpoint("srchDtlList", new Reply(503, ""), new Reply(503, ""));
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK.replace("\"bookDtlUrl\"", "\"authors\":\"작가 지음\",\"bookImageURL\":\"https://example.org/img\",\"bookDtlUrl\""));
        assertThat(catalog.lookup(ISBN).getAuthor()).isEqualTo("작가");
        assertThat(requests.stream().filter(q -> q.startsWith("/srchDtlList")).count()).isEqualTo(2);
    }

    @Test
    void malformedSearchPayloadIsAnOutage() {
        ok("srchBooks", "{\"response\":{}}");
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> client.keyword("단어", 1, 10));
    }

    @Test
    void invalidIsbnFailsWithoutProviderCall() {
        expect(BookErrorCode.INVALID_ISBN, () -> catalog.lookup("9788936434268"));
        assertThat(requests).isEmpty();
    }

    @Test
    void conflictingBlockedEvidenceFromEitherProviderRejectsBook() {
        ok("srchDtlList", DETAIL);
        ok("srchBooks", LINK);
        ok("SearchApi.do", NATIONAL.replace("03810", "73810"));
        expect(BookErrorCode.BLOCKED_CATEGORY, () -> catalog.lookup(ISBN));
    }

    @Test
    void searchRetainsHttpContractRawPaginationAndOrderAfterFiltering() throws Exception {
        String blocked = BOOK.replace(ISBN, "9788936434120").replace("\"class_no\"", "\"addition_symbol\":\"63810\",\"class_no\"");
        ok("srchBooks", "{\"response\":{\"numFound\":11,\"docs\":[{\"doc\":" + blocked
                + "},{\"doc\":" + BOOK + "},{\"doc\":" + BOOK + "}]}}");
        var mvc = MockMvcBuilders.standaloneSetup(new AladinController(new AladinService(client, categories, authors))).build();
        mvc.perform(get("/api/books/search").param("keyword", "한 글&").param("page", "2").param("size", "3"))
                .andExpect(status().isOk()).andExpect(content().json("""
                        {"isSuccess":true,"code":"COMMON200","message":"성공적으로 요청을 처리했습니다.",
                        "result":{"totalResults":11,"totalPage":4,"books":[{"title":"제목","author":"김작가",
                        "image":"https://example.org/img","publisher":"","isbn13":"9788936434267","link":"",
                        "category":"KOREAN_NOVEL","categoryLabel":"한국소설"}]}}
                        """, JsonCompareMode.STRICT));
        assertThat(requests).singleElement().asString().contains("keyword=한 글&", "pageNo=2", "pageSize=3");
    }

    @Test
    void duplicateCannotReintroduceBlockedIsbn() {
        String second = BOOK.replace("\"class_no\"", "\"addition_symbol\":\"73810\",\"class_no\"");
        ok("srchBooks", "{\"response\":{\"numFound\":2,\"docs\":[{\"doc\":" + BOOK + "},{\"doc\":" + second + "}]}}");
        var result = new AladinService(client, categories, authors).searchBooks("제목", 1, 10);
        assertThat(result.getBooks()).isEmpty();
        assertThat(result.getTotalResults()).isEqualTo(2);
    }

    @Test
    void emptyPageKeepsTotalAndDefaultParameters() throws Exception {
        ok("srchBooks", "{\"response\":{\"numFound\":0,\"docs\":[]}}");
        var mvc = MockMvcBuilders.standaloneSetup(new AladinController(new AladinService(client, categories, authors))).build();
        mvc.perform(get("/api/books/search").param("keyword", "제목"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.totalPage").value(0))
                .andExpect(jsonPath("$.result.books").isEmpty());
        assertThat(requests).singleElement().asString().contains("pageNo=1", "pageSize=10");
    }

    @Test
    void quotaErrorDoesNotRetryOrExposeCredential() {
        endpoint("srchBooks", new Reply(429, credential));
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> client.keyword("books", 1, 10));
        assertThat(requests).hasSize(1);
    }

    @Test
    void malformedUrlOrAuthenticatedUrlIsNotBookMetadata() {
        assertThat(CatalogBook.externalUrl("javascript:alert(1)")).isEmpty();
        assertThat(CatalogBook.externalUrl("https://example.org/api?authKey=private")).isEmpty();
        assertThat(CatalogBook.externalUrl("https://user@example.org/")).isEmpty();
    }

    private void expect(BookErrorCode code, org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work).isInstanceOfSatisfying(BookException.class, ex -> {
            assertThat(ex.getCode()).isEqualTo(code);
            assertThat(ex.getCause()).isNull();
            assertThat(ex.toString()).doesNotContain(credential);
        });
    }
}
