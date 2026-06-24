package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record TrackerDetailResDTO(
        Long groupId,
        String groupName,
        TradeType tradeType,
        RoleStatus myRole,

        @Schema(description = "서버가 계산한 화면 표시 상태", example = "REVIEW_WRITING")
        TrackerDisplayStatus displayStatus,
        @Schema(description = "현재 상태에서 대표로 표시할 원본 책 제목. 말줄임과 상태 라벨 조합은 프론트에서 처리", example = "떡볶이 사주 - 따끈하게 풀어낸 쉬운 사주 이야기")
        String displayBookTitle,
        @Schema(description = "화면 표시 상태 라벨. 책 제목과 조합하지 않은 라벨 단독 값", example = "후기 작성")
        String displayStatusLabel,
        Integer dDay,

        BookInfo myBook,
        BookInfo partnerBook,

        List<TrackerStepInfo> steps
) {}
