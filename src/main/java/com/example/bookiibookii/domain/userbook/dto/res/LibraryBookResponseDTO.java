package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LibraryBookResponseDTO {
    private Long userBookId;
    private Long bookId;
    private String title;
    private String author;
    private String image;   // 책 이미지
    private Long hostId;
    private String hostProfileImageUrl;  // presigned GET URL
    private LocalDate startDate;        // group.startDate (시작일)
    private Integer duration;            // group.readingPeriod (독서 기간, 일)
    private Double rating;
    private String comment;
}
