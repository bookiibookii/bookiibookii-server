package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackerListItemResDTO {

    // 그룹의 메타데이터
    private Long groupId;
    private String groupName;
    private TradeType tradeType;
    private RoleStatus myRole;

    // 매칭멤버의 ReadingStatus, ExchangeStatus로 조립
    @Schema(description = "서버가 계산한 화면 표시 상태", example = "REVIEW_WRITING")
    private TrackerDisplayStatus displayStatus;

    @Schema(description = "현재 상태에서 대표로 표시할 원본 책 제목", example = "떡볶이 사주 - 따끈하게 풀어낸 쉬운 사주 이야기")
    private String displayBookTitle;

    @Schema(description = "화면 표시 상태 라벨", example = "후기 작성")
    private String displayStatusLabel;

    private Integer remainingDays; // 배송중이면 null, D-day
    private BookInfo myCurrentBook;
    private BookInfo partnerCurrentBook;

}