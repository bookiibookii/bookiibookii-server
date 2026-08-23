package com.example.bookiibookii.domain.aladin.config;

import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.json.JsonMapper;

final class AladinResponseParser {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build();

    AladinClient.AladinItemSearchResponse parseItemSearchResponse(String rawBody) {
        return objectMapper.readValue(rawBody, AladinClient.AladinItemSearchResponse.class);
    }
}
