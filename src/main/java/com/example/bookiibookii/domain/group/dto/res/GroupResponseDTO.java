package com.example.bookiibookii.domain.group.dto.res;

import com.example.bookiibookii.domain.group.dto.RuleDTO;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


public class GroupResponseDTO {

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateResultDTO{
        private Long groupId;
        private GroupStatus groupStatus;
        private String createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateResultDTO {
        private Long groupId; // 수정된 그룹 ID
        private String updatedAt; // 수정 완료 시각 (포맷팅된 문자열)
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteResultDTO{
        private Long groupId;
        private String deletedAt; //삭제된 완료시각
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupDetailDTO {
        // 1. 그룹 및 상태 정보
        private Long groupId;
        private String groupStatus;    // RECRUITING, MATCHED
        private Boolean isHost;        // 조회자가 방장인지 여부
        private String tradeType;      // DIRECT, DELIVERY
        private String preferRegion;

        // 2. 도서 상세 정보 (Book 엔티티와 매핑)
        private String title;
        private String bookImage;
        private String author;
        private String genre;          // CustomCategory 명칭

        // 3. 그룹 설정 및 배지 정보
        private Integer readingPeriod; // 독서 기간 (day)
        private Integer matchedCount;  // 현재 확정 인원 (MatchedMember 수)
        private Integer maxCapacity;   // 정원
        private Integer waitingCount; // 대기자 수
        private Boolean isHot;         // 대기자가 정원의 3배 이상인지 여부 (Service에서 계산)
        private String createdAt;
        private String startDate;

        // 4. 호스트 정보 및 규칙
        private String hostNickname;
        private String hostProfileImageUrl;  // 프로필 이미지 Presigned GET URL

        // 5. 그룹 소개 및 참여 멤버 슬롯
        private String groupComment;   // 그룹 소개글

        private String groupName;
        private List<RuleDTO> rules;

        // 예: 정원 4명 중 2명 참여 시 -> [방장, 게스트1, EMPTY, EMPTY] 순서로 구성
        private List<ParticipantSlotDTO> participantSlots;

        // APPLY(신청하기), CANCEL(취소하기), MANAGE(요청관리), TRACKER(트래커보기), FULL(인원마감)
        private String buttonStatus;

    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantSlotDTO {
        private String nickname;       // 유저 닉네임 (빈 슬롯이면 null)
        private String profileImageUrl;   // 프로필 이미지 Presigned GET URL (빈 슬롯이면 null)
        private String role;           // HOST, GUEST, EMPTY(빈자리)
        private Boolean isMe;          // 본인 여부
    }

    @Builder
    public record GroupSummaryDTO(
            Long groupId,
            String groupName,
            String title,
            String author,
            String genre,
            String bookImage,
            String hostNickname,
            String hostProfileImageUrl,
            String groupStatus,
            int currentCount,
            int maxCapacity,
            int waitingCount,
            boolean isHot,
            String tradeType,
            Integer readingPeriod,
            String pictureBadge
    ) {}

    public record GroupSliceResponseDTO(
            List<GroupSummaryDTO> groupList,
            int currentPage,
            boolean hasNext
    ) {}

    public record SearchResultDTO(
            List<GroupSummaryDTO> groupList,
            Long totalCount,
            int currentPage,
            boolean hasNext
    ) {}
    // 드롭다운용 그룹 데이터 (신고하기 API의 신고그룹 조회)
    @Builder
    public record GroupSummaryResponse(
            Long groupId,
            String groupName,
            String groupHostNickname,
            boolean isHost
    ) {}

    // 드롭다운용 그룹멤버 데이터 (신고하기 API의 신고 멤버 조회)
    @Builder
    public record GroupMemberResponse(
            Long userId,
            String nickname
    ) {}

    @Builder
    public record MypageGroupDto  (
            Long groupId,
            String bookTitle,
            String auth,
            @JsonProperty("GENRE")
            String genre,
            @JsonProperty("group_status")
            GroupStatus groupStatus,
            List<String> groupTags
    ){}

    // ===== 그룹 홈 화면 (GET /api/groups/home) =====

    /** 홈 화면 전 섹션 공통 그룹 카드 */
    @Builder
    public record HomeGroupCardDTO(
            Long groupId,
            String groupName,
            String hostNickname,
            String hostProfileImageUrl,
            String bookImage,
            String bookTitle,
            String author,
            Integer readingPeriod
    ) {}

    /** 섹션2 — 카테고리 추천 그룹. category가 null이면 추천 가능한 카테고리 없음 */
    @Builder
    public record CategorySectionDTO(
            String category,
            List<HomeGroupCardDTO> groups
    ) {}

    /** 섹션5 — 위치 기반 직접교환 그룹. region이 null이면 사용자 교환 장소 미설정 */
    @Builder
    public record RegionSectionDTO(
            String region,
            List<HomeGroupCardDTO> groups
    ) {}

    /** 섹션3 — 베스트셀러 기반 그룹 추천 */
    @Builder
    public record BestsellerSectionDTO(
            List<HomeGroupCardDTO> groups
    ) {}

    @Builder
    public record HomeResponseDTO(
            List<HomeGroupCardDTO> newGroups,
            CategorySectionDTO categorySection,
            BestsellerSectionDTO bestsellerSection,
            RegionSectionDTO regionSection
    ) {}
}
