package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tag.enums.TagType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class GroupRequestDTO {

    @Getter
    //그룹생성 req
    public static class CreateDTO{
        @NotBlank(message = "ISBN은 필수 입력 사항입니다.")
        @Pattern(regexp = "^[0-9]{13}$", message = "ISBN13은 숫자 13자리여야 합니다.")
        private String isbn13;          // 대상 도서 ID
        private Integer maxCapacity;   // TOGETHER일 때 인원수
        private LocalDate startDate;
        private Integer readingPeriod;
        @NotBlank(message = "그룹 소개는 필수 입력 사항입니다.")
        @Size(max = 500, message = "소개글은 최대 500자까지 입력 가능합니다.")
        private String groupComment;
        @Size(max = 8)
        private String customTag;
        private GroupType groupType;   // RELAY, TOGETHER
        private TradeType tradeType;   // DELIVERY, DIRECT
        private String preferRegion;
        private String meetPlace;
        private List<GroupRequestDTO.TagSettingDTO> tags;
    }

    @Getter
    public static class UpdateDTO{
            private LocalDate startDate;
            private Integer readingPeriod;
            private String groupComment;
            @Size(max = 8)
            private String customTag;
            @Valid
            private List<GroupRequestDTO.TagSettingDTO> tags;
    }

    public record FilterDTO(
            List<GroupType> groupTypes,
            List<TradeType> tradeTypes,
            List<String> meetPlace,
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

    public record SearchDTO(
            @NotBlank(message = "검색어를 입력해주세요.")
            @Schema(description = "검색어", example = "자바") // 예시 단어 설정
            String keyword,

            @Schema(description = "정렬 방식", defaultValue = "LATEST")
            GroupSortType sort,

            @Min(0)
            @Schema(description = "페이지 번호", defaultValue = "0") // 스웨거 기본값 설정
            Integer page,

            @Positive
            @Schema(description = "페이지 크기", defaultValue = "10") // 스웨거 기본값 설정
            Integer size
    ){
        public SearchDTO {
            if (page == null) page = 0;
            if (size == null) size = 10;
            if (sort == null) sort = GroupSortType.LATEST;
        }}
}


