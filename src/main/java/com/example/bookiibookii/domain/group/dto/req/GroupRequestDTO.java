package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.dto.RuleDTO;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class GroupRequestDTO {

    @Getter
    //그룹생성 req
    public static class CreateDTO{
        @NotBlank(message = "ISBN은 필수 입력 사항입니다.")
        @Pattern(regexp = "^[0-9]{13}$", message = "ISBN13은 숫자 13자리여야 합니다.")
        private String isbn13;          // 대상 도서 ID
        @Schema(description = "모집 인원 (2~8명)", example = "4")
        @Min(2)
        @Max(8)
        private Integer maxCapacity;
        @Schema(description = "독서 시작 날짜 (오늘 이후)", example = "2026-02-20")
        private LocalDate startDate;
        @Schema(description = "독서 기간 (7, 14, 21, 28일 중 택1)", example = "14")
        private Integer readingPeriod;
        @Schema(description = "그룹 소개글 (최대 500자)", example = "숭실대 근처에서 같이 경제 서적 읽으실 분 구해요!")
        @Size(max = 500)
        private String groupComment;
        @Schema(description = "그룹 타입 (RELAY: 이어읽기, TOGETHER: 함께읽기)", example = "TOGETHER")
        private GroupType groupType;   // RELAY, TOGETHER
        @Schema(description = "교환 방식 (DIRECT: 직거래, NONE: 함께읽기 시)", example = "DIRECT")
        private TradeType tradeType;   // DIRECT, NONE
        @Schema(description = "선호 지역 (직거래 시 필수)", example = "서울 동작구 상도동")
        private String preferRegion;
        @Schema(description = "상세 만남 장소 (직거래 시)", example = "상도역 1번 출구 스타벅스")
        private String meetPlace;
        // TOGETHER 타입 전용
        @Schema(description = "그룹명 (TOGETHER 타입 필수)", example = "같이 읽어요")
        private String groupName;
        @Schema(description = "규칙 리스트 (1~5개, TOGETHER 타입 필수)")
        @Size(min = 1, max = 5)
        @Valid
        private List<@NotNull RuleDTO> rules;
    }

    @Getter
    public static class UpdateDTO{
        @Schema(description = "수정할 시작 날짜", example = "2026-02-25")
        private LocalDate startDate;
        @Schema(description = "수정할 독서 기간", example = "21")
        private Integer readingPeriod;
        @Schema(description = "수정할 소개글", example = "내용을 조금 수정했습니다. 끝까지 함께하실 분!")
        private String groupComment;
        @Schema(description = "수정할 규칙 리스트 (1~5개, TOGETHER/직접교환 타입)")
        @Size(min = 1, max = 5)
        @Valid
        private List<@NotNull RuleDTO> rules;
    }

    public record FilterDTO(
            @Schema(description = "그룹 타입 필터 ", example = "[\"TOGETHER\"]")
            List<GroupType> groupTypes,
            @Schema(description = "거래 방식 필터 ", example = "[\"DIRECT\"]")
            List<TradeType> tradeTypes,
            @Schema(description = "장소 필터 (지역 이름 리스트)", example = "[\"동작구\", \"관악구\"]")
            List<String> meetPlace,
            @Schema(description = "카테고리 필터 (도서 장르)", example = "[\"ECON_BIZ\", \"SOCIETY\"]")
            List<CustomCategory> categories,
            @Schema(description = "정렬 방식 (LATEST: 최신순, POPULAR: 인기순, RECOMMEND: 추천순)", example = "LATEST")
            GroupSortType sort, // LATEST, POPULAR, RECOMMEND
            @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
            @Min(0) int page,
            @Schema(description = "한 페이지에 불러올 개수", example = "10")
            @Positive int size
            ) {}

    public record SearchDTO(
            @NotBlank(message = "검색어를 입력해주세요.")
            @Schema(description = "검색어", example = "자바")
            String keyword,

            @Schema(description = "정렬 방식", defaultValue = "LATEST")
            GroupSortType sort,

            @Min(0)
            @Schema(description = "페이지 번호", defaultValue = "0")
            Integer page,

            @Positive
            @Schema(description = "페이지 크기", defaultValue = "10")
            Integer size
    ){
        public SearchDTO {
            if (page == null) page = 0;
            if (size == null) size = 10;
            if (sort == null) sort = GroupSortType.LATEST;
        }}
}
