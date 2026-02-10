package com.example.bookiibookii.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class KeywordReqDTO {

    public record SaveKeyword(
            @NotBlank(message = "키워드를 입력해주세요.")
            @Size(max = 50, message = "키워드는 50자 이내로 입력해주세요.")
            String content
    ) {}

    public record DeleteKeyword(
            @NotNull
            Long keywordId
    ) {}
}
