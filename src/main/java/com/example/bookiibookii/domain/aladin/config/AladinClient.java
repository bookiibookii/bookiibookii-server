package com.example.bookiibookii.domain.aladin.config;

import com.example.bookiibookii.domain.aladin.exception.code.AladinErrorCode;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AladinClient {

    private static final String ALADIN_BASE_URL = "https://www.aladin.co.kr/ttb/api";

    @Value("${aladin.ttbkey}")
    private String aladinKey;

    private final RestClient restClient;

    public AladinClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(ALADIN_BASE_URL)
                .build();
    }

    public AladinItemSearchResponse searchBooksByKeyword(String keyword, int page, int size) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/ItemSearch.aspx")
                        .queryParam("ttbkey", aladinKey)
                        .queryParam("Query", keyword)
                        .queryParam("start", page)
                        .queryParam("MaxResults", size)
                        .queryParam("SubSearchTarget", "Book")
                        .queryParam("cover", "Big")
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
            throw new BookException(AladinErrorCode.ALADIN_NOT_FOUND);
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
