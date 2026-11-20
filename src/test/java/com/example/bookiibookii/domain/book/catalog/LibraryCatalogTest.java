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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
            requests.add(exchange.getRequestURI().toString().replace(credential, "[hidden]"));
            var reply = queue.size() > 1 ? queue.removeFirst() : queue.element();
            byte[] body = reply.body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status, body.length);
            try (var stream = exchange.getResponseBody()) { stream.write(body); }
        });
    }

    private void ok(String path, String body) { endpoint(path, new Reply(200, body)); }

    @Test
    void mergesOnlyExactEditionsAndUsesAnActualReturnedDetailLink() {
        ok("srchDtlList", DETAIL.replace("https://example.org/img", "http://image.aladin.co.kr/cover.jpg"));
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK);
        var b = catalog.lookup(ISBN);
        assertThat(b.getTitle()).isEqualTo("제목");
        assertThat(b.getAuthor()).isEqualTo("김작가");
        assertThat(b.getPublisher()).isEqualTo("출판사");
        assertThat(b.getImage()).isEqualTo("https://image.aladin.co.kr/cover.jpg");
        assertThat(b.getTotalPages()).isEqualTo(320);
        assertThat(b.getLink()).isEqualTo("https://example.org/edition");
        assertThat(b.getCategory()).isEqualTo(CustomCategory.KOREAN_NOVEL);
        assertThat(client.naruDetail(ISBN).get(0).description()).isEqualTo("소개");
        assertThat(requests).anySatisfy(q -> assertThat(q).startsWith("/srchBooks?")
                .contains("isbn13=" + ISBN, "pageNo=1", "pageSize=10")
                .doesNotContain("title=", "keyword="));
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
        assertThat(catalog.lookup(ISBN).getLink()).isEmpty();
    }

    @Test
    void incompleteNationalPageCannotAuthorizeRegistrationEvenWithAnExactMatch() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL.replace("\"2\"", "\"11\""));
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> catalog.lookup(ISBN));
    }

    @Test
    void incompleteNaruPageCannotAuthorizeRegistrationEvenWithAnExactMatch() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK.replace("\"numFound\":1", "\"numFound\":11"));
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> catalog.lookup(ISBN));
    }

    @Test
    void laterExactNaruRecordCanBlockAnEarlierAllowedMatch() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK.replace("\"numFound\":1", "\"numFound\":2")
                .replace("]}}", ",{\"doc\":{\"isbn13\":\"" + ISBN + "\",\"addition_symbol\":\"93510\"}}]}}"));
        expect(BookErrorCode.BLOCKED_CATEGORY, () -> catalog.lookup(ISBN));
    }

    @Test
    void oversizedOptionalLinkIsOmitted() {
        ok("srchDtlList", DETAIL);
        ok("SearchApi.do", NATIONAL);
        ok("srchBooks", LINK.replace("edition", "a".repeat(256)));
        assertThat(catalog.lookup(ISBN).getLink()).isEmpty();
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
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> client.searchByTitle("단어", 1, 10));
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

    @ParameterizedTest
    @CsvSource({
            "나미야, 나미야 잡화점의 기적, 9788972756194",
            "프로젝트 헤일메리, 프로젝트 헤일메리, 9788925588735",
            "눈물, 눈물점, 9788998791995"
    })
    void searchesByTitleAndReturnsMatchingBookThroughPublicKeywordParameter(String keyword, String title, String isbn)
            throws Exception {
        ok("srchBooks", """
                {"response":{"numFound":1,"docs":[{"doc":{
                    "bookname":"%s","isbn13":"%s","class_no":"843",
                    "bookImageURL":"http://image.aladin.co.kr/cover.jpg"
                }}]}}
                """.formatted(title, isbn));
        var mvc = MockMvcBuilders.standaloneSetup(new AladinController(new AladinService(client, categories, authors))).build();

        mvc.perform(get("/api/books/search").param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.books[0].title").value(title))
                .andExpect(jsonPath("$.result.books[0].isbn13").value(isbn))
                .andExpect(jsonPath("$.result.books[0].category").value("WORLD_NOVEL"))
                .andExpect(jsonPath("$.result.books[0].image").value("https://image.aladin.co.kr/cover.jpg"));
        assertThat(requests).singleElement().asString().startsWith("/srchBooks?")
                .contains("title=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8) + "&")
                .doesNotContain("keyword=");
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
        assertThat(requests).singleElement().asString()
                .contains("title=" + URLEncoder.encode("한 글&", StandardCharsets.UTF_8) + "&", "pageNo=2", "pageSize=3")
                .doesNotContain("keyword=");
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
        expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> client.searchByTitle("books", 1, 10));
        assertThat(requests).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({
            "HTTP://IMAGE.ALADIN.CO.KR/a%20b.jpg?v=a%2Fb#cover, https://IMAGE.ALADIN.CO.KR/a%20b.jpg?v=a%2Fb#cover",
            "http://image.aladin.co.kr:80/cover.jpg, https://image.aladin.co.kr/cover.jpg",
            "http://image.aladin.co.kr:8080/cover.jpg, http://image.aladin.co.kr:8080/cover.jpg",
            "https://image.aladin.co.kr/cover.jpg, https://image.aladin.co.kr/cover.jpg",
            "http://image.aladin.co.kr.example.org/cover.jpg, http://image.aladin.co.kr.example.org/cover.jpg",
            "http://www.aladin.co.kr/cover.jpg, http://www.aladin.co.kr/cover.jpg",
            "http://example.org/cover.jpg, http://example.org/cover.jpg"
    })
    void upgradesOnlyVerifiedAladinCoverUrls(String original, String normalized) {
        assertThat(CatalogBook.coverUrl(original)).isEqualTo(normalized);
        assertThat(CatalogBook.externalUrl(original)).isEqualTo(original);
    }

    @Test
    void malformedUrlOrAuthenticatedUrlIsNotBookMetadata() {
        assertThat(CatalogBook.externalUrl("javascript:alert(1)")).isEmpty();
        assertThat(CatalogBook.externalUrl("https://example.org/api?authKey=private")).isEmpty();
        assertThat(CatalogBook.externalUrl("https://user@example.org/")).isEmpty();
    }

    @Test
    void stalledBodyTimesOutAndReleasesProviderPermit() {
        var started = new CountDownLatch(1);
        server.createContext("/stall", exchange -> {
            exchange.sendResponseHeaders(200, 10);
            exchange.getResponseBody().write('{');
            exchange.getResponseBody().flush();
            started.countDown();
            // Leave the response open until the client cancels.
        });
        ok("healthy", "{\"ok\":true}");
        var transport = new LibraryTransport(new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/", "key", credential, 0, Duration.ofSeconds(1));
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            expect(BookErrorCode.CATALOG_UNAVAILABLE, () -> transport.get("stall", Map.of()));
            assertThat(started.getCount()).isZero();
            assertThat(transport.get("healthy", Map.of()).path("ok").asBoolean()).isTrue();
        });
    }

    private void expect(BookErrorCode code, org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work).isInstanceOfSatisfying(BookException.class, ex -> {
            assertThat(ex.getCode()).isEqualTo(code);
            assertThat(ex.getCause()).isNull();
            assertThat(ex.toString()).doesNotContain(credential);
        });
    }
}
