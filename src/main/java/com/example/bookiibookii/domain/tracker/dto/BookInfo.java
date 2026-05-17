package com.example.bookiibookii.domain.tracker.dto;

import lombok.Builder;

@Builder
public record BookInfo(
        String title,
        String image,
        Integer totalPages,

        boolean isOwnerBook, // 현재 읽는 사람 기준으로 본인 소유 책인지
        String currentReaderNickname,
        String currentReaderProfileImageUrl,
        int currentReadingRate
){}
