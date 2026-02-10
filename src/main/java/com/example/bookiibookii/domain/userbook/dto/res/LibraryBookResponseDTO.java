package com.example.bookiibookii.domain.userbook.dto.res;

import com.example.bookiibookii.domain.group.enums.GroupType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LibraryBookResponseDTO {
    private Long groupId;
    private Long userBookId;
    private Long bookId;
    private String title;
    private String author;
    private String image;   // 책 이미지
    private Long hostId;
    private String hostNickName;        // 호스트 닉네임
    private String hostProfileImageUrl;  // presigned GET URL
    private GroupType groupType;         // group.groupType (TOGETHER, RELAY)
    private LocalDate startDate;        // group.startDate (시작일)
    private LocalDate endDate;             // 그룹 종료일자
    private Integer duration;            // group.readingPeriod (독서 기간, 일)
    private Double rating;
    private String comment;
}
