package com.example.bookiibookii.domain.aladin.config;

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

    public AladinSearchRawResponse searchBooksJson(String keyword, int page, int size) {
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
                .body(AladinSearchRawResponse.class);
    }

    // json response by Aladin
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinSearchRawResponse(
            @JsonProperty("totalResults") int totalResults,
            @JsonProperty("item") List<AladinBookItem> item
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AladinBookItem(
            @JsonProperty("title") String title,
            @JsonProperty("author") String author,
            @JsonProperty("priceStandard") Long priceStandard,
            @JsonProperty("cover") String cover,
            @JsonProperty("publisher") String publisher,
            @JsonProperty("pubDate") String pubDate,
            @JsonProperty("isbn13") String isbn13,
            @JsonProperty("categoryName") String categoryName,
            @JsonProperty("description") String description
    ) {}
}
