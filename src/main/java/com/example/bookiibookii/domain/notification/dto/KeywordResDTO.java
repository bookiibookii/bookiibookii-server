package com.example.bookiibookii.domain.notification.dto;

import com.example.bookiibookii.domain.notification.enums.KeywordSort;
import lombok.Builder;

import java.util.List;

public class KeywordResDTO {

    @Builder
    public record KeywordList(
            KeywordSort keywordSort,
            int keywordNumber,
            List<KeywordItem> keywordList
    ){}

    @Builder
    public record KeywordItem(
        Long keywordId,
        String content
    ){}
}
