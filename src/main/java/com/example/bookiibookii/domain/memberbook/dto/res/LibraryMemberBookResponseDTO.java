package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class LibraryMemberBookResponseDTO {
    private Long groupId;
    private Long memberBookId;
    /** MatchedMember 기준 본인 책 여부 (false면 상대/호스트 책) */
    private boolean isMine;
    private Long bookId;
    private String title;
    private String author;
    private String image;
    private Long hostId;
    private String hostNickName;
    private String hostProfileImageUrl;
    private GroupType groupType;
    private GroupStatus groupStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private Double progressRate;
    private Double rating;
    private String comment;
}
