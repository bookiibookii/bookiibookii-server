package com.example.bookiibookii.domain.nlgoKr.config;

import com.example.bookiibookii.domain.nlgoKr.exception.SeojiException;
import com.example.bookiibookii.domain.nlgoKr.exception.code.SeojiErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@Slf4j
public class SeojiClient {

    private static final String SEOJI_BASE_URL = "https://www.nl.go.kr/seoji";

    @Value("${nlgoKr.certKey}")
    private String certKey;

    private final RestClient restClient;

    public SeojiClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(SEOJI_BASE_URL)
                .build();
    }

    public SeojiSearchResponse searchBooks(String keyword, int page, int size) {
        SeojiSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/SearchApi.do")
                        .queryParam("cert_key", certKey)
                        .queryParam("result_style", "json")
                        .queryParam("page_no", page)
                        .queryParam("page_size", size)
                        .queryParam("title", keyword)
                        .build())
                .retrieve()
                .body(SeojiSearchResponse.class);

        if (response == null) {
            log.warn("[Seoji] 검색 응답 없음 keyword={}", keyword);
            return new SeojiSearchResponse("0", List.of());
        }
        return response;
    }

    public SeojiBookItem lookupBookByIsbn13(String isbn13) {
        SeojiSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/SearchApi.do")
                        .queryParam("cert_key", certKey)
                        .queryParam("result_style", "json")
                        .queryParam("page_no", 1)
                        .queryParam("page_size", 1)
                        .queryParam("isbn", isbn13)
                        .build())
                .retrieve()
                .body(SeojiSearchResponse.class);

        if (response == null || response.docs() == null || response.docs().isEmpty()) {
            log.warn("[Seoji] 책 조회 결과 없음 isbn13={}", isbn13);
            throw new SeojiException(SeojiErrorCode.SEOJI_NOT_FOUND);
        }
        return response.docs().get(0);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeojiSearchResponse(
            @JsonProperty("TOTAL_COUNT") String totalCount,
            @JsonProperty("docs") List<SeojiBookItem> docs
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeojiBookItem(
            @JsonProperty("TITLE") String title,
            @JsonProperty("AUTHOR") String author,
            @JsonProperty("PUBLISHER") String publisher,
            @JsonProperty("EA_ISBN") String isbn13,
            @JsonProperty("EA_ADD_CODE") String addCode,    // ISBN 부가기호 (7=아동, 5=중고참고서, 6=초등참고서)
            @JsonProperty("TITLE_URL") String titleUrl,     // 표지 이미지 URL
            @JsonProperty("PAGE") String page,              // 페이지 수
            @JsonProperty("KDC") String kdc,                // 한국십진분류 코드
            @JsonProperty("BOOK_INTRODUCTION") String bookIntroduction,
            @JsonProperty("PUBLISH_PREDATE") String publishDate
    ) {
        /** PAGE 필드가 없거나 숫자가 아닐 경우 null 반환 */
        public Integer pageCount() {
            if (page == null || page.isBlank()) return null;
            try {
                return Integer.parseInt(page.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
