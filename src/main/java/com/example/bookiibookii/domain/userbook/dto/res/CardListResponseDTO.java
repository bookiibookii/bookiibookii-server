package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
public class CardListResponseDTO {
    private Long groupId;       // 그룹 ID (그룹 멤버 간 공유 조회용)
    /** 트래커 현재 주자 (RELAY 등). 없으면 null (TOGETHER 또는 트래커 미생성) */
    private CurrentBookOwnerDto currentBookOwner;
    /** RELAY일 때만: 요청한 사용자(나)의 UserBook 한줄평. null 허용 */
    private String myComment;
    /** RELAY일 때만: 현재 주자(상대)의 UserBook 한줄평. 내가 주자면 null. null 허용 */
    private String partnerComment;
    /** TOGETHER일 때만: 그룹 멤버들의 UserBook 한줄평 리스트. null 허용 */
    private List<TogetherCommentDto> togetherComments;
    private List<GroupCardResponseDTO> cards; // 각 카드에 bookTitle, creatorName 등 포함

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentBookOwnerDto {
        private Long matchedMemberId;
        private String nickname;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TogetherCommentDto {
        private Long userId;
        private String nickname;
        private String comment;
    }
}
