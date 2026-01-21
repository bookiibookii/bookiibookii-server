package com.example.bookiibookii.domain.group.dto.res;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


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
}
