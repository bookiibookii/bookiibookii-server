package com.example.bookiibookii.domain.tracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record BookInfo(
        String title,
        String image,
        Integer totalPages,
        Integer currentPage,

        @Schema(description = "현재 로그인 유저가 이 그룹에 원래 등록한 책인지 여부")
        boolean isMyOriginalBook,
        String currentReaderNickname,
        String currentReaderProfileImageUrl,
        int currentReadingRate
){}
