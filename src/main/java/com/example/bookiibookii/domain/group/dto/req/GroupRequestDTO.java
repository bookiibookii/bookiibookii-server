package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GroupRequestDTO {

    @Getter
    //그룹생성 req
    public static class CreateDTO{
        private String isbn13;          // 대상 도서 ID
        private Integer maxCapacity;   // TOGETHER일 때 인원수
        private LocalDate startDate;
        private Integer readingPeriod;
        private String groupComment;
        private GroupType groupType;   // RELAY, TOGETHER
        private TradeType tradeType;   // DELIVERY, DIRECT
        // 독서 태그 리스트 (예: ["#메모환영", "#깔끔"]) 추가 필요
    }

    @Getter
    public static class UpdateDTO{
            private LocalDate startDate;
            private Integer readingPeriod;
            private String groupComment;
            //태그리스트
    }

    public record FilterDTO(
            List<GroupType> groupTypes,
            List<TradeType> tradeTypes,
            List<String> regions,
            List<CustomCategory> categories,
            String sort, // LATEST, POPULAR, RECOMMEND
            int page,
            int size
    ) {}

}


