package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tag.enums.TagType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
        private String customTag;
        private GroupType groupType;   // RELAY, TOGETHER
        private TradeType tradeType;   // DELIVERY, DIRECT
        private List<GroupRequestDTO.TagSettingDTO> tags;
    }

    @Getter
    public static class UpdateDTO{
            private LocalDate startDate;
            private Integer readingPeriod;
            private String groupComment;
            private String customTag;
            @Valid
            private List<GroupRequestDTO.TagSettingDTO> tags;
    }

    public record FilterDTO(
            List<GroupType> groupTypes,
            List<TradeType> tradeTypes,
            List<String> regions,
            List<CustomCategory> categories,
            GroupSortType sort, // LATEST, POPULAR, RECOMMEND
            @Min(0) int page,
            @Positive int size
            ) {}

    public record TagSettingDTO (
            @NotNull
            TagType type,

            @NotEmpty
            List<String> value
    ){}
}


