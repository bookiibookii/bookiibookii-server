package com.example.bookiibookii.domain.user.dto.res;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class BookshelfResponseDTO {

    @Builder
    public record BookshelfResDTO(
            List<CompletedBookDto> completedBooks,
            List<FavoriteBookDto> favoriteBooks,
            List<RepresentativeBookDto> representativeBooks
    ) {}

    // 완독 + 리뷰 완료한 책
    public record CompletedBookDto(
            Long groupBookId,
            String title,
            String author,
            String image,
            String category,
            Double rating,
            LocalDate completedAt
    ) {}

    // 인생 책
    public record FavoriteBookDto(
            Long userBookId,
            String title,
            String author,
            String category,
            String image
    ) {}

    // 대표 책
    public record RepresentativeBookDto(
            Long userBookId,
            String title,
            Integer displayOrder,
            boolean isFavorite
    ) {}
}
