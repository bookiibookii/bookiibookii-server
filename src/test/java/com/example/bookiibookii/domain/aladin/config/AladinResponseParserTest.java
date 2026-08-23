package com.example.bookiibookii.domain.aladin.config;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AladinResponseParserTest {

    private final AladinResponseParser parser = new AladinResponseParser();

    @Test
    void parsesValidAladinJson() {
        String json = """
                {
                  "totalResults": 1,
                  "item": [{
                    "title": "정상 도서",
                    "author": "저자",
                    "isbn13": "9780000000001",
                    "categoryId": 141
                  }]
                }
                """;

        AladinClient.AladinItemSearchResponse response = parser.parseItemSearchResponse(json);

        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.item()).singleElement()
                .satisfies(item -> assertThat(item.title()).isEqualTo("정상 도서"));
    }

    @Test
    void parsesNonStandardBackslashEscapeInDescription() {
        String json = """
                {
                  "totalResults": 1,
                  "item": [{
                    "title": "도산 안창호 평전",
                    "author": "신용하",
                    "description": "도산의 참된 상 像\\을 오랜 연구 끝에 비로소 세상에 내놓는다.",
                    "isbn13": "9788942390885",
                    "categoryId": 141
                  }]
                }
                """;

        AladinClient.AladinItemSearchResponse response = parser.parseItemSearchResponse(json);

        assertThat(response.item()).singleElement()
                .satisfies(item -> assertThat(item.title()).isEqualTo("도산 안창호 평전"));
    }

    @Test
    void rejectsStructurallyBrokenJson() {
        assertThatThrownBy(() -> parser.parseItemSearchResponse("{\"item\":["))
                .isInstanceOf(JacksonException.class);
    }
}
