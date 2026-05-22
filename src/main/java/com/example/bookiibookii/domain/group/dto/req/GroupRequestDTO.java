package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.dto.RuleDTO;
import com.example.bookiibookii.domain.group.enums.GroupSortType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.util.List;

public class GroupRequestDTO {

    @Getter
    //그룹생성 req
    public static class CreateDTO {
        @NotBlank(message = "ISBN은 필수 입력 사항입니다.")
        @Pattern(regexp = "^[0-9]{13}$", message = "ISBN13은 숫자 13자리여야 합니다.")
        private String isbn13;
        @Schema(description = "독서 기간 (3, 7, 14, 21, 28일 중 택1)", example = "14")
        private Integer readingPeriod;
        @Schema(description = "그룹 소개글 (선택, 최대 500자)", example = "숭실대 근처에서 같이 경제 서적 읽으실 분 구해요!")
        @Size(max = 500)
        private String groupComment;
        @NotNull(message = "교환 방식은 필수입니다.")
        @Schema(description = "교환 방식 (DIRECT: 직거래, DELIVERY: 택배 교환)", example = "DIRECT")
        private TradeType tradeType;
        @Schema(description = "그룹 생성 시 선택한 장소 id. DELIVERY는 내 배송지 id, DIRECT는 내 희망교환장소 id입니다.", example = "1")
        private Long selectedPlaceId;
        @NotBlank(message = "그룹명은 필수입니다.")
        @Schema(description = "그룹명", example = "같이 읽어요")
        private String groupName;
        @Schema(description = "규칙 리스트 (1~5개, 독서 스타일 태그 1개 이상 필수)")
        @Size(min = 1, max = 5)
        @Valid
        private List<@NotNull RuleDTO> rules;
    }

    @Getter
    public static class UpdateDTO{
        @Schema(description = "수정할 독서 기간", example = "21")
        private Integer readingPeriod;
        @Schema(description = "수정할 소개글", example = "내용을 조금 수정했습니다. 끝까지 함께하실 분!")
        private String groupComment;
        @Schema(description = "수정할 그룹명", example = "같이 읽어요")
        private String groupName;
        @Schema(description = "수정할 규칙 리스트 (1~5개)")
        @Size(min = 1, max = 5)
        @Valid
        private List<@NotNull RuleDTO> rules;
    }

    public record FilterDTO(
            @Schema(description = "거래 방식 필터 (DIRECT: 직거래, DELIVERY: 택배)", example = "[\"DIRECT\"]")
            List<TradeType> tradeTypes,
            @Schema(description = "지역 필터 (시+구 형태 리스트)", example = "[\"인천시 미추홀구\", \"인천시 부평구\"]")
            List<String> regions,
            @Schema(description = "카테고리 필터 (도서 장르)", example = "[\"ECON_BIZ\", \"SOCIETY\"]")
            List<CustomCategory> categories,
            @Schema(description = "정렬 방식 (LATEST: 최신순)", example = "LATEST")
            GroupSortType sort,
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
