package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LibraryBookResponseDTO {
    private Long userBookId;
    private Long bookId;
    private String title;
    private String author;
    private String image;
    private Long hostId;
    private String hostProfileImageUrl;  // presigned GET URL
    private Integer duration;            // group.readingPeriod (독서 기간, 일)
    private Double rating;
    private String comment;
}
