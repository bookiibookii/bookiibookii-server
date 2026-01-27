package com.example.bookiibookii.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class KeywordReqDTO {

    public record SaveKeyword(
            @NotBlank
            String content
    ) {}

    public record DeleteKeyword(
            @NotNull
            Long keywordId
    ) {}
}
