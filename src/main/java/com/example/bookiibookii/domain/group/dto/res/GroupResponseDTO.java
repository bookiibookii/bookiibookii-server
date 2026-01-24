package com.example.bookiibookii.domain.group.dto.res;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
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
        private String title;          // 도서 제목
        private String groupStatus;    // RECRUITING, MATCHED
        private Boolean isHost;        // 조회자가 방장인지 여부

        // 2. 도서 상세 정보 (Book 엔티티와 매핑)
        private String bookTitle;
        private String bookImage;
        private String author;
        private String category;       // CustomCategory 명칭

        // 3. 그룹 설정 및 배지 정보
        private Integer readingPeriod; // 독서 기간 (day)
        private Integer matchedCount;  // 현재 확정 인원 (MatchedMember 수)
        private Integer maxCapacity;   // 정원
        private Integer waitingCount; // 대기자 수
        private Boolean isHot;         // 대기자가 정원의 3배 이상인지 여부 (Service에서 계산)

        // 4. 호스트 정보 및 태그
        private String hostNickname;
        private String hostProfileImage;
        private String createdAt;      // "2024. 01. 24" 형식 (호스트 계정 생성일?)

        // TODO: 그룹 태그 리스트 (현재 GroupTag 엔티티와 매핑 필요)
        // Groups 엔티티의 List<GroupTag>를 순회하며 Tag의 name들을 추출해 담아야 합니다.
        //private List<String> tags;

        // 5. 그룹 소개 및 참여 멤버 슬롯
        private String groupComment;   // 그룹 소개글


        // 예: 정원 4명 중 2명 참여 시 -> [방장, 게스트1, EMPTY, EMPTY] 순서로 구성
        private List<ParticipantSlotDTO> participantSlots;

        // 6. 하단 버튼 상태 (프론트엔드 버튼 분기용)
        // APPLY(신청하기), CANCEL(취소하기), MANAGE(요청관리), TRACKER(트래커보기), FULL(인원마감)
        private String buttonStatus;

        // 댓글 리스트
        // private List<CommentResponseDTO> comments;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantSlotDTO {
        private String nickname;       // 유저 닉네임 (빈 슬롯이면 null)
        private String profileImage;   // 프로필 이미지 (빈 슬롯이면 null)
        private String role;           // HOST, GUEST, EMPTY(빈자리)
        private Boolean isMe;          // 본인 여부
    }
}
