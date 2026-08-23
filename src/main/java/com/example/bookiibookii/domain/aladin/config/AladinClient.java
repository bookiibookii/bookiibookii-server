package com.example.bookiibookii.domain.aladin.config;

import com.example.bookiibookii.domain.aladin.exception.AladinException;
import com.example.bookiibookii.domain.aladin.exception.code.AladinErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;

import java.util.List;

@Component
@Slf4j
public class AladinClient {

    private static final String ALADIN_BASE_URL = "https://www.aladin.co.kr/ttb/api";

    @Value("${aladin.ttbkey}")
    private String aladinKey;

    private final RestClient restClient;
    private final AladinResponseParser responseParser;

    public AladinClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(ALADIN_BASE_URL)
                .build();
        this.responseParser = new AladinResponseParser();
    }

    public AladinItemSearchResponse searchBooksByKeyword(String keyword, int page, int size) {
        String rawBody = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemSearch.aspx")
                        .queryParam("ttbkey", aladinKey)
                        .queryParam("Query", keyword)
                        .queryParam("start", page)
                        .queryParam("MaxResults", size)
                        .queryParam("SearchTarget", "Book")
                        .queryParam("cover", "Big")
                        .queryParam("output", "JS")
                        .queryParam("Version", "20131101")
                        .build())
                .retrieve()
                .body(String.class);

        try {
            return responseParser.parseItemSearchResponse(rawBody);
        } catch (JacksonException ex) {
            Throwable rootCause = rootCauseOf(ex);
            log.error("Failed to parse Aladin search response: keyword={}, page={}, size={}, cause={}",
                    keyword, page, size, rootCause.getMessage());
            throw new AladinException(AladinErrorCode.ALADIN_RESPONSE_PARSE_ERROR, ex);
        }
    }

    private Throwable rootCauseOf(Throwable throwable) {
        Throwable rootCause = throwable;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    public AladinItemSearchResponse fetchBestsellers(int maxResults) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemList.aspx")
                        .queryParam("ttbkey", aladinKey)
                        .queryParam("QueryType", "Bestseller")
                        .queryParam("MaxResults", maxResults)
                        .queryParam("SearchTarget", "Book")
                        .queryParam("output", "JS")
                        .queryParam("Version", "20131101")
                        .build())
                .retrieve()
                .body(AladinItemSearchResponse.class);
    }

    public AladinBookItem lookupBookByIsbn13(String isbn13) {
        AladinItemLookupResponse raw = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemLookUp.aspx")
                        .queryParam("ttbkey", aladinKey)
                        .queryParam("itemIdType", "ISBN13")
                        .queryParam("ItemId", isbn13)
                        .queryParam("SubSearchTarget", "Book")
                        .queryParam("cover", "Big")
                        .queryParam("output", "JS")
                        .queryParam("Version", "20131101")
                        .build())
                .retrieve()
                .body(AladinItemLookupResponse.class);

        if (raw.item() == null || raw.item().isEmpty()) {
            throw new AladinException(AladinErrorCode.ALADIN_NOT_FOUND);
        }
        return raw.item().get(0);
    }

    // json response by Aladin - 여러 건 조회
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinItemSearchResponse(
            @JsonProperty("totalResults") int totalResults,
            @JsonProperty("item") List<AladinBookItem> item
    ) {}

    // json response by Aladin - 단건 조회(알라딘-단건도 list 형태로 내려주는 구조)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinItemLookupResponse(
            @JsonProperty("item") List<AladinBookItem> item
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinBookItem(
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("cover") String cover,
            @JsonProperty("publisher") String publisher,
            @JsonProperty("pubDate") String pubDate,
            @JsonProperty("isbn13") String isbn13,
            @JsonProperty("link") String link,
            @JsonProperty("categoryId") Long categoryId,
            @JsonProperty("categoryName") String categoryName,
            @JsonProperty("subInfo") SubInfo subInfo // isbn13 조회
    ) {
        public Integer itemPage() {
            return subInfo != null ? subInfo.itemPage() : null;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record SubInfo(
                @JsonProperty("itemPage") Integer itemPage
        ) {}
    }
}
